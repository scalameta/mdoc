package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import scala.meta._
import scala.meta.inputs.Position
import mdoc.Reporter
import mdoc.document.CompileResult
import mdoc.document.CrashResult
import mdoc.document.CrashResult.Crashed
import mdoc.internal.document.MdocExceptions
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance

object Renderer {

  def render(
      sections: List[Input],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String
  ): String = {
    val inputs =
      sections.map(s => SectionInput(s, dialects.Sbt1(s).parse[Source].get, Modifier.Default))
    val instrumented = Instrumenter.instrument(inputs)
    val doc =
      MarkdownCompiler.buildDocument(compiler, reporter, inputs, instrumented, filename)
    doc.sections
      .map(s => s"""```scala
                   |${Renderer.renderEvaluatedSection(doc, s, reporter)}
                   |```""".stripMargin)
      .mkString("\n")
  }

  def renderCrashSection(
      section: EvaluatedSection,
      reporter: Reporter,
      edit: TokenEditDistance
  ): String = {
    require(section.mod.isCrash, section.mod)
    val out = new ByteArrayOutputStream()
    val ps = new PrintStream(out)
    ps.println("```scala")
    ps.println(section.source.syntax)
    val crashes = for {
      statement <- section.section.statements
      binder <- statement.binders
      if binder.value.isInstanceOf[Crashed]
    } yield binder.value.asInstanceOf[Crashed]
    crashes.headOption match {
      case Some(CrashResult.Crashed(e, _)) =>
        MdocExceptions.trimStacktrace(e)
        val stacktrace = new ByteArrayOutputStream()
        e.printStackTrace(new PrintStream(stacktrace))
        appendFreshMultiline(ps, stacktrace.toString())
        ps.append('\n')
      case None =>
        val mpos = section.source.pos.toUnslicedPosition
        reporter.error(mpos, "Expected runtime exception but program completed successfully")
    }
    ps.println("```")
    out.toString()
  }

  def appendMultiline(sb: PrintStream, string: String): Unit = {
    val isTrailing = string.endsWith("\n")
    var i = 0
    val N = if (isTrailing) string.length - 1 else string.length
    while (i < N) {
      string.charAt(i) match {
        case '\n' =>
          sb.append("\n// ")
        case ch => sb.append(ch)
      }
      i += 1
    }
  }

  def appendFreshMultiline(sb: PrintStream, string: String): Unit = {
    sb.append("// ")
    appendMultiline(sb, string)
  }

  def renderEvaluatedSection(
      doc: EvaluatedDocument,
      section: EvaluatedSection,
      reporter: Reporter
  ): String = {
    val baos = new ByteArrayOutputStream()
    val sb = new PrintStream(baos)
    var first = true
    section.section.statements.zip(section.source.stats).foreach {
      case (statement, tree) =>
        if (first) {
          first = false
        } else {
          sb.append("\n")
        }
        sb.append(tree.syntax)
        if (statement.out.nonEmpty) {
          sb.append("\n")
          appendFreshMultiline(sb, statement.out)
        }
        sb.append("\n")

        statement.binders.foreach { binder =>
          section.mod match {
            case Modifier.Fail =>
              binder.value match {
                case CompileResult.TypecheckedOK(_, tpe, pos) =>
                  val mpos = Position
                    .Range(
                      section.input,
                      pos.startLine,
                      pos.startColumn,
                      pos.endLine,
                      pos.endColumn
                    )
                    .toUnslicedPosition
                  reporter.error(
                    mpos,
                    s"Expected compile error but statement type-checked successfully"
                  )
                  appendMultiline(sb, tpe)
                case CompileResult.ParseError(msg, pos) =>
                  appendFreshMultiline(sb, pos.formatMessage(doc.edit, msg))
                case CompileResult.TypeError(msg, pos) =>
                  appendFreshMultiline(sb, pos.formatMessage(doc.edit, msg))
                case _ =>
                  val obtained = pprint.PPrinter.BlackWhite.apply(binder).toString()
                  throw new IllegalArgumentException(
                    s"Expected Macros.CompileResult." +
                      s"Obtained $obtained"
                  )
              }
            case Modifier.Default | Modifier.Passthrough =>
              val lines = pprint.PPrinter.BlackWhite.tokenize(binder.value)
              if (binder.name.startsWith("res") && binder.tpe.render == "Unit") {
                () // do nothing
              } else {
                sb.append("// ")
                  .append(binder.name)
                  .append(": ")
                  .append(binder.tpe.render)
                  .append(" = ")
                lines.foreach { lineStr =>
                  val line = lineStr.plainText
                  appendMultiline(sb, line)
                }
                sb.append("\n")
              }

            case Modifier.Crash =>
              throw new IllegalArgumentException(Modifier.Crash.toString)
            case c: Modifier.Str =>
              throw new IllegalArgumentException(c.toString)
          }
        }
    }
    baos.toString.trim
  }

}
