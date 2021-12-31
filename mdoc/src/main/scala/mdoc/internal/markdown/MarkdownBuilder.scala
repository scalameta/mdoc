package mdoc.internal.markdown

import mdoc.Reporter
import mdoc.document.Document
import mdoc.document._
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.document.DocumentBuilder
import mdoc.internal.document.MdocNonFatal
import mdoc.internal.CompatClassloader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

object MarkdownBuilder {

  def default(): MarkdownCompiler = fromClasspath(classpath = "", scalacOptions = "")

  def buildDocument(
      compiler: MarkdownCompiler,
      reporter: Reporter,
      sectionInputs: List[SectionInput],
      instrumented: Instrumented,
      filename: String
  ): EvaluatedDocument = {
    val instrumentedInput = InstrumentedInput(filename, instrumented.source)
    reporter.debug(s"$filename: instrumented code\n$instrumented")
    val compileInput = Input.VirtualFile(filename, instrumented.source)
    val edit = SectionInput.tokenEdit(sectionInputs, compileInput)
    val compiled = compiler.compile(
      compileInput,
      reporter,
      edit,
      "repl.MdocSession$",
      instrumented.fileImports
    )
    val doc = compiled match {
      case Some(cls) =>
        val ctor = cls.getDeclaredConstructor()
        ctor.setAccessible(true)
        val doc = ctor.newInstance().asInstanceOf[DocumentBuilder].$doc
        var evaluatedDoc = Document.empty(instrumentedInput)
        runInClassLoader(cls.getClassLoader()) { () =>
          try {
            evaluatedDoc = doc.build(instrumentedInput)
          } catch {
            case e: DocumentException =>
              val index = e.sections.length - 1
              val input = sectionInputs(index).input
              val pos =
                if (e.pos.isEmpty) {
                  Position.Range(input, 0, 0)
                } else {
                  val slice = Position.Range(
                    input,
                    e.pos.startLine,
                    e.pos.startColumn,
                    e.pos.endLine,
                    e.pos.endColumn
                  )
                  slice.toUnslicedPosition
                }
              reporter.error(pos, e.getCause)
              evaluatedDoc = Document(instrumentedInput, e.sections)
            case MdocNonFatal(e) =>
              reporter.error(e)
              evaluatedDoc = Document.empty(instrumentedInput)
          }
        }
        evaluatedDoc
      case None =>
        // An empty document will render as the original markdown
        Document.empty(instrumentedInput)
    }
    EvaluatedDocument(doc, sectionInputs)
  }

  def fromClasspath(classpath: String, scalacOptions: String): MarkdownCompiler = {
    val fullClasspath =
      if (classpath.isEmpty) defaultClasspath(_ => true)
      else {
        val base = Classpath(classpath)
        val runtime = defaultClasspath(path => {
          val pathString = path.toString
          pathString.contains("scala-library") ||
          pathString.contains("scala3-library") ||
          pathString.contains("scala-reflect") ||
          pathString.contains("fansi") ||
          pathString.contains("pprint") ||
          pathString.contains("mdoc-interfaces") ||
          (pathString.contains("mdoc") && pathString.contains("runtime")) ||
          (pathString.contains("mdoc") && pathString.contains("printing"))
        })
        base ++ runtime
      }
    new MarkdownCompiler(fullClasspath.syntax, scalacOptions)
  }

  private def runInClassLoader(classloader: ClassLoader)(f: () => Unit) = {
    val thread = new Thread {
      override def run: Unit = {
        f()
      }
    }
    thread.setContextClassLoader(classloader)
    thread.start()
    thread.join()
  }

  private def defaultClasspath(fn: Path => Boolean): Classpath = {
    val paths =
      CompatClassloader
        .getURLs(getClass.getClassLoader)
        .iterator
        .map(url => AbsolutePath(Paths.get(url.toURI)))
        .filter(p => fn(p.toNIO))
        .toList
    Classpath(paths)
  }

}
