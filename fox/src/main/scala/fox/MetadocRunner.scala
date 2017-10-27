package fox

import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream
import scala.collection.{GenSeq, concurrent}
import java.nio.file.attribute.BasicFileAttributes
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.function.{Function => JFunction}
import scala.collection.mutable
import scala.collection.parallel.mutable.ParArray
import scala.meta.tokens.Token
import scala.util.control.NonFatal
import caseapp._
import caseapp.core.Messages
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions
import metadoc.cli.TermDisplay
import metadoc.schema
import metadoc.schema.SymbolIndex
import metadoc.{schema => d}
import org.langmeta._
import org.{langmeta => m}
import org.langmeta.internal.semanticdb.{schema => s}

case class Target(target: AbsolutePath, onClose: () => Unit)
case class SymbolData(
    symbol: m.Symbol.Global,
    definition: m.Position.Range,
    denotation: m.Denotation,
    docstring: Option[Token.Comment]
)

class MetadocRunner(classpath: Seq[AbsolutePath], options: MetadocOptions) {
  val Target(target, onClose) = if (options.zip) {
    // For large corpora (>1M LOC) writing the symbol/ directory is the
    // bottle-neck unless --zip is enabled.
    val out = options.targetPath.resolve("metadoc.zip")
    Files.createDirectories(out.toNIO.getParent)
    val zipfs = FileSystems.newFileSystem(
      URI.create(s"jar:file:${out.toURI.getPath}"), {
        val env = new util.HashMap[String, String]()
        env.put("create", "true")
        env
      }
    )
    Target(AbsolutePath(zipfs.getPath("/")), () => zipfs.close())
  } else {
    Target(options.targetPath, () => ())
  }
  private val display = new TermDisplay(
    new OutputStreamWriter(System.out),
    fallbackMode = options.nonInteractive || TermDisplay.defaultFallbackMode
  )
  private val semanticdb = target.resolve("semanticdb")
  private val symbolRoot = target.resolve("symbol")
  private type Symbol = String
  private val filenames = new ConcurrentSkipListSet[String]()
  private val symbols =
    new ConcurrentHashMap[Symbol, AtomicReference[d.SymbolIndex]]()
  private val denotations =
    new ConcurrentHashMap[Symbol, s.Denotation]()
  private val files =
    new ConcurrentHashMap[Symbol, m.Input.VirtualFile]()
  private val symbolMappingFunction: JFunction[Symbol, AtomicReference[
    d.SymbolIndex
  ]] =
    t => new AtomicReference(d.SymbolIndex(symbol = t))

  private def overwrite(out: Path, bytes: Array[Byte]): Unit = {
    Files.write(
      out,
      bytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE
    )
  }

  private def addDefinition(symbol: Symbol, position: d.Position): Unit = {
    val value = symbols.computeIfAbsent(symbol, symbolMappingFunction)
    value.getAndUpdate(new UnaryOperator[d.SymbolIndex] {
      override def apply(t: schema.SymbolIndex): schema.SymbolIndex =
        t.definition.fold(t.copy(definition = Some(position))) { _ =>
          // Do nothing, conflicting symbol definitions, for example js/jvm
          t
        }
    })
  }

  private def addReference(
      filename: String,
      range: d.Range,
      symbol: Symbol
  ): Unit = {
    val value = symbols.computeIfAbsent(symbol, symbolMappingFunction)
    value.getAndUpdate(new UnaryOperator[d.SymbolIndex] {
      override def apply(t: d.SymbolIndex): d.SymbolIndex = {
        val ranges = t.references.getOrElse(filename, d.Ranges())
        val newRanges = ranges.copy(ranges.ranges :+ range)
        val newReferences = t.references + (filename -> newRanges)
        t.copy(references = newReferences)
      }
    })
  }

  type Tick = () => Unit

  private def phase[T](task: String, length: Int)(f: Tick => T): T = {
    display.startTask(task, new File("target"))
    display.taskLength(task, length, 0)
    val counter = new AtomicInteger()
    val tick: Tick = { () =>
      display.taskProgress(task, counter.incrementAndGet())
    }
    val result = f(tick)
    display.completedTask(task, success = true)
    result
  }

  def scanSemanticdbs(): GenSeq[AbsolutePath] =
    phase("Scanning semanticdb files", classpath.length) { tick =>
      val files = ParArray.newBuilder[AbsolutePath]
      val visitor = new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (file.getFileName.toString.endsWith(".semanticdb")) {
            files += AbsolutePath(file)
          }
          FileVisitResult.CONTINUE
        }
      }
      classpath.foreach { path =>
        tick()
        Files.walkFileTree(path.toNIO, visitor)
      }
      files.result()
    }

  def buildSymbolIndex(paths: GenSeq[AbsolutePath]): Unit =
    phase("Building symbol index", paths.length) { tick =>
      paths.foreach { path =>
        try {
          tick()
          val bytes = path.readAllBytes
          val db = s.Database.parseFrom(bytes)
          db.documents.foreach { document =>
            document.names.foreach {
              case s.ResolvedName(_, sym, _)
                  if !sym.endsWith(".") && !sym.endsWith("#") =>
              // Do nothing, local symbol.
              case s.ResolvedName(Some(s.Position(start, end)), sym, true) =>
                addDefinition(sym, d.Position(document.filename, start, end))
              case s.ResolvedName(Some(s.Position(start, end)), sym, false) =>
                addReference(document.filename, d.Range(start, end), sym)
              case _ =>
            }
            document.symbols.foreach {
              case s.ResolvedSymbol(sym, Some(denot)) =>
                denotations.putIfAbsent(sym, denot)
              case _ =>
            }
            files.putIfAbsent(
              document.filename,
              Input.VirtualFile(document.filename, document.contents)
            )
            val out = semanticdb.resolve(document.filename)
            Files.createDirectories(out.toNIO.getParent)
            overwrite(
              out.toNIO
                .resolveSibling(
                  out.toNIO.getFileName.toString + ".semanticdb"
                ),
              s.Database(document :: Nil).toByteArray
            )
            filenames.add(document.filename)
          }
        } catch {
          case NonFatal(e) =>
            System.err.println(s"$path")
            val shortTrace = e.getStackTrace.take(10)
            e.setStackTrace(shortTrace)
            e.printStackTrace(new PrintStream(System.err))
        }
      }
    }

  def writeSymbolIndex(): Unit =
    phase("Writing symbol index", symbols.size()) { tick =>
      import scala.collection.JavaConverters._
      Files.createDirectory(symbolRoot.toNIO)
      val symbolsMap = symbols.asScala
      symbolsMap.foreach {
        case (_, ref) =>
          tick()
          val symbolIndex = ref.get()
          val actualIndex = symbolIndex.definition match {
            case Some(_) => updateReferencesForType(symbolsMap, symbolIndex)
            case None => updateDefinitionsForTerm(symbolsMap, symbolIndex)
          }

          if (actualIndex.definition.isDefined) {
            val url = MetadocCli.encodeSymbolName(actualIndex.symbol)
            val out = symbolRoot.resolve(url)
            overwrite(out.toNIO, actualIndex.toByteArray)
          }
      }
    }

  private def updateReferencesForType(
      symbolsMap: concurrent.Map[Symbol, AtomicReference[SymbolIndex]],
      symbolIndex: SymbolIndex
  ) = {
    Symbol(symbolIndex.symbol) match {
      case Symbol.Global(owner, Signature.Type(name)) =>
        (for {
          syntheticObjRef <- symbolsMap.get(
            Symbol.Global(owner, Signature.Term(name)).syntax
          )
          if syntheticObjRef.get().definition.isEmpty
        } yield symbolIndex.copy(references = syntheticObjRef.get().references))
          .getOrElse(symbolIndex)
      case _ => symbolIndex
    }
  }

  private def updateDefinitionsForTerm(
      symbolsMap: concurrent.Map[Symbol, AtomicReference[SymbolIndex]],
      symbolIndex: SymbolIndex
  ) = {
    Symbol(symbolIndex.symbol) match {
      case Symbol.Global(owner, Signature.Term(name)) =>
        (for {
          typeRef <- symbolsMap.get(
            Symbol.Global(owner, Signature.Type(name)).syntax
          )
          definition <- typeRef.get().definition
        } yield symbolIndex.copy(definition = Some(definition)))
          .getOrElse(symbolIndex)
      case _ => symbolIndex
    }
  }

  private def writeAssets(): Unit = {
    val root = target.toNIO
    val inputStream = MetadocCli.getClass.getClassLoader
      .getResourceAsStream("metadoc-assets.zip")
    if (inputStream == null)
      sys.error("Failed to locate metadoc-assets.zip on the classpath")
    val zipStream = new ZipInputStream(inputStream)
    val bytes = new Array[Byte](8012)
    Stream
      .continually(zipStream.getNextEntry)
      .takeWhile(_ != null)
      .filterNot(_.isDirectory)
      .foreach { entry =>
        val path = root.resolve(entry.getName)
        if (Files.notExists(path))
          Files.createDirectories(path.getParent)
        val out = Files.newOutputStream(path, StandardOpenOption.CREATE)

        def copyLoop(): Unit = {
          val read = zipStream.read(bytes, 0, bytes.length)
          if (read > 0) {
            out.write(bytes, 0, read)
            copyLoop()
          }
        }

        copyLoop()
        out.flush()
        out.close()
      }
  }

  private def writeWorkspace(): Unit = {
    import scala.collection.JavaConverters._
    val workspace = d.Workspace(filenames.asScala.toSeq)
    overwrite(target.resolve("index.workspace").toNIO, workspace.toByteArray)
  }

  def run(): MetadocIndex = {
    try {
      display.init()
      Files.createDirectories(target.toNIO)
      val paths = scanSemanticdbs()
      buildSymbolIndex(paths)
      writeSymbolIndex()
      writeAssets()
      writeWorkspace()
      new MetadocIndex(files, symbols, denotations)
    } finally {
      display.stop()
      onClose()
    }
  }
}

class MetadocIndex(
    files: ConcurrentHashMap[String, m.Input.VirtualFile],
    symbols: ConcurrentHashMap[String, AtomicReference[d.SymbolIndex]],
    denotations: ConcurrentHashMap[String, s.Denotation]
) {
  import scala.collection.JavaConverters._
  def symbolData: mutable.Iterable[SymbolData] = {
    object R {
      def unapply[T](arg: AtomicReference[T]): Option[T] =
        Option(arg.get)
    }
    object S {
      def unapply(sym: String): Option[(m.Symbol, m.Denotation)] =
        for {
          denot <- denotation(sym)
          symbol <- m.Symbol.unapply(sym)
        } yield symbol -> denot
    }
    object P {
      def unapply(defn: d.Position): Option[m.Position.Range] =
        for {
          input <- Option(files.get(defn.filename))
        } yield m.Position.Range(input, defn.start, defn.end)
    }
    symbols.asScala.collect {
      case (
          S(symbol: m.Symbol.Global, denot),
          R(SymbolIndex(_, Some(P(defn)), _))
          ) =>
        SymbolData(symbol, defn, denot, None)
    }
  }

  def denotation(symbol: String): Option[m.Denotation] =
    for {
      denot <- Option(denotations.get(symbol))
    } yield m.Denotation(denot.flags, denot.name, denot.signature, Nil)

  def definition(symbol: String): Option[d.Position] =
    for {
      indexRef <- Option(symbols.get(symbol))
      index = indexRef.get()
      defn <- index.definition
    } yield defn
}
