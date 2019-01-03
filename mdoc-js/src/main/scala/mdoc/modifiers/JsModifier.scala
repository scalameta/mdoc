package mdoc.modifiers

import mdoc.OnLoadContext
import mdoc.PostProcessContext
import mdoc.PreModifierContext
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.livereload.Resources
import mdoc.internal.markdown.CodeBuilder
import mdoc.internal.markdown.Gensym
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance
import org.scalajs.core.tools.io.IRFileCache
import org.scalajs.core.tools.io.IRFileCache.VirtualRelativeIRFile
import org.scalajs.core.tools.io.MemVirtualSerializedScalaJSIRFile
import org.scalajs.core.tools.io.VirtualScalaJSIRFile
import org.scalajs.core.tools.io.WritableMemVirtualJSFile
import org.scalajs.core.tools.linker.Linker
import org.scalajs.core.tools.linker.StandardLinker
import org.scalajs.core.tools.logging.Level
import org.scalajs.core.tools.logging.Logger
import org.scalajs.core.tools.sem.Semantics
import scala.collection.mutable.ListBuffer
import scala.meta.Term
import scala.meta.inputs.Input
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.reflect.io.VirtualDirectory

class JsModifier extends mdoc.PreModifier {
  override val name = "js"
  val irCache = new IRFileCache
  val target = new VirtualDirectory("(memory)", None)
  var mountNode = "node"
  var maybeCompiler: Option[MarkdownCompiler] = None
  var linker: Linker = newLinker()
  var virtualIrFiles: Seq[VirtualRelativeIRFile] = Nil
  var classpathHash: Int = 0
  var reporter: mdoc.Reporter = new ConsoleReporter(System.out)
  var gensym = new Gensym()
  var outDirectory: AbsolutePath = PathIO.workingDirectory
  var fullOpt = false
  var minLevel: Level = Level.Info
  val sjsLogger: Logger = new Logger {
    override def log(level: Level, message: => String): Unit = {
      if (level >= minLevel) {
        if (level == Level.Warn) reporter.info(message)
        else if (level == Level.Error) reporter.info(message)
        else reporter.info(message)
      }
    }
    override def success(message: => String): Unit =
      reporter.info(message)
    override def trace(t: => Throwable): Unit =
      reporter.error(t)
  }

  private val runs = ListBuffer.empty[String]
  private val inputs = ListBuffer.empty[Input]

  def reset(): (List[String], List[Input]) = {
    val result = (runs.toList, inputs.toList)
    runs.clear()
    inputs.clear()
    gensym.reset()
    result
  }

  def newLinker(): Linker = {
    val semantics =
      if (fullOpt) Semantics.Defaults.optimized
      else Semantics.Defaults
    StandardLinker(
      StandardLinker
        .Config()
        .withSemantics(semantics)
        .withSourceMap(false)
        .withClosureCompilerIfAvailable(fullOpt)
    )
  }

  override def onLoad(ctx: OnLoadContext): Unit = {
    (ctx.site.get("js-classpath"), ctx.site.get("js-scalacOptions")) match {
      case (None, None) => // nothing to do
      case (Some(_), None) =>
        ctx.reporter.error("missing key: 'js-scalacOptions'")
      case (None, Some(_)) =>
        ctx.reporter.error("missing key: 'js-classpath'")
      case (Some(classpath), Some(scalacOptions)) =>
        fullOpt = ctx.site.get("js-opt") match {
          case None =>
            !ctx.settings.watch
          case Some(value) =>
            value match {
              case "fast" => false
              case "full" => true
              case unknown =>
                ctx.reporter.error(s"unknown 'js-opt': $unknown")
                !ctx.settings.watch
            }
        }
        val newClasspathHash = (classpath, scalacOptions, fullOpt).hashCode()
        // Reuse the  linker and compiler when the classpath+scalacOptions haven't changed
        // to speed up unit tests by nearly 2x.
        if (classpathHash != newClasspathHash) {
          linker = newLinker()
          classpathHash = newClasspathHash
          maybeCompiler = Some(new MarkdownCompiler(classpath, scalacOptions, target))
          virtualIrFiles = {
            val irContainer =
              IRFileCache.IRContainer.fromClasspath(Classpath(classpath).entries.map(_.toFile))
            val cache = irCache.newCache
            cache.cached(irContainer)
          }
        }
        mountNode = ctx.site.getOrElse("js-mountNode", mountNode)
        outDirectory = ctx.site.get("js-out-prefix") match {
          case Some(value) =>
            // This is needed for Docusaurus that requires assets (non markdown) files to live under
            // `docs/assets/`: https://docusaurus.io/docs/en/doc-markdown#linking-to-images-and-other-assets
            ctx.settings.out.resolve(value)
          case None =>
            ctx.settings.out
        }
        reporter = ctx.reporter
        minLevel = ctx.site.get("js-level") match {
          case None => Level.Info
          case Some("info") => Level.Info
          case Some("warn") => Level.Warn
          case Some("error") => Level.Error
          case Some("debug") => Level.Debug
          case Some(unknown) =>
            reporter.warning(s"unknown 'js-level': $unknown")
            Level.Info
        }
    }
  }

  override def postProcess(ctx: PostProcessContext): String = {
    if (runs.isEmpty) {
      reset()
      ""
    } else {
      maybeCompiler match {
        case None =>
          ctx.reporter.error(
            inputs.head.toPosition,
            "Can't process `mdoc:js` code fence because Scala.js is not configured. " +
              "To fix this problem, set the site variables `js-classpath` and `js-scalacOptions`. " +
              "If you are using sbt-mdoc, update the `mdocJS` setting to point to a Scala.js project."
          )
          ""
        case Some(compiler) =>
          postProcess(ctx, compiler)
      }
    }
  }

  def postProcess(ctx: PostProcessContext, compiler: MarkdownCompiler): String = {
    val (runs, inputs) = reset()
    val code = new CodeBuilder()
    val wrapped = code
      .println("object mdocjs {")
      .foreach(runs)(code.println)
      .println("}")
      .toString
    val input = Input.VirtualFile(ctx.relativePath.toString(), wrapped)
    val edit = TokenEditDistance.fromInputs(inputs, input)
    val oldErrors = ctx.reporter.errorCount
    compiler.compileSources(input, ctx.reporter, edit)
    val hasErrors = ctx.reporter.errorCount > oldErrors
    val sjsir = for {
      x <- target.toList
      if x.name.endsWith(".sjsir")
    } yield {
      val f = new MemVirtualSerializedScalaJSIRFile(x.path)
      f.content = x.toByteArray
      f: VirtualScalaJSIRFile
    }
    if (sjsir.isEmpty) {
      if (!hasErrors) {
        ctx.reporter.error("Scala.js compilation failed")
      }
      ""
    } else {
      val output = WritableMemVirtualJSFile("output.js")
      linker.link(virtualIrFiles ++ sjsir, Nil, output, sjsLogger)
      val outfile = outDirectory.resolve(ctx.relativePath.resolveSibling(_ + ".js"))
      outfile.write(output.content)
      val outmdoc = outfile.resolveSibling(_ => "mdoc.js")
      outmdoc.write(Resources.readPath("/mdoc.js"))
      val relfile = outfile.toRelativeLinkFrom(ctx.outputFile)
      val relmdoc = outmdoc.toRelativeLinkFrom(ctx.outputFile)
      new CodeBuilder()
        .println(s"""<script type="text/javascript" src="$relfile" defer></script>""")
        .println(s"""<script type="text/javascript" src="$relmdoc" defer></script>""")
        .toString
    }
  }

  override def process(ctx: PreModifierContext): String = {
    JsMods.parse(ctx.infoInput, ctx.reporter) match {
      case Some(mods) =>
        process(ctx, mods)
      case None =>
        ""
    }
  }

  def process(ctx: PreModifierContext, mods: JsMods): String = {
    val separator = "\n---\n"
    val text = ctx.originalCode.text
    val separatorIndex = text.indexOf(separator)
    val (body, input) =
      if (separatorIndex < 0) {
        ("", ctx.originalCode)
      } else {
        val sliced = Input.Slice(
          ctx.originalCode,
          separatorIndex + separator.length,
          ctx.originalCode.chars.length
        )
        (
          text.substring(0, separatorIndex),
          sliced
        )
      }
    val run = gensym.fresh("run")
    inputs += input
    val id = s"mdoc-js-$run"
    val mountNodeParam = Term.Name(mountNode)
    val code: String =
      if (mods.isShared) {
        input.text
      } else {
        new CodeBuilder()
          .println(s""" @_root_.scala.scalajs.js.annotation.JSExportTopLevel("$id") """)
          .println(s"""def $run($mountNodeParam: _root_.org.scalajs.dom.raw.Element): Unit = {""")
          .println(input.text)
          .println("}")
          .toString
      }
    runs += code
    new CodeBuilder()
      .printlnIf(!mods.isInvisible, s"```scala\n${input.text}\n```")
      .printlnIf(!mods.isShared, s"""<div id="$id" data-mdoc-js>$body</div>""")
      .toString
  }
}
