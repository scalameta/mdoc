package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import mdoc.Reporter
import mdoc.Variable
import mdoc.document.CompileResult
import mdoc.document.CrashResult
import mdoc.document.CrashResult.Crashed
import mdoc.internal.document.MdocExceptions
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance
import scala.meta._
import scala.meta.inputs.Position

object Renderer {

  def render(
      sections: List[Input],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String,
      printer: Variable => String
  ): String = {
    val inputs =
      sections.map(s => SectionInput(s, dialects.Sbt1(s).parse[Source].get, Modifier.Default))
    val instrumented = Instrumenter.instrument(inputs)
    val doc =
      MarkdownCompiler.buildDocument(compiler, reporter, inputs, instrumented, filename)
    doc.sections
      .map(s => s"""```scala
                   |${Renderer.renderEvaluatedSection(doc, s, reporter, printer)}
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
    appendMultiline(sb, string, string.length)
  }
  def appendMultiline(sb: PrintStream, string: String, N: Int): Unit = {
    var i = 0
    while (i < N) {
      string.charAt(i) match {
        case '\n' =>
          sb.append("\n// ")
        case ch =>
          sb.append(ch)
      }
      i += 1
    }
  }

  def appendFreshMultiline(sb: PrintStream, string: String): Unit = {
    val N = string.length - (if (string.endsWith("\n")) 1 else 0)
    sb.append("// ")
    appendMultiline(sb, string, N)
  }

  def renderEvaluatedSection(
      doc: EvaluatedDocument,
      section: EvaluatedSection,
      reporter: Reporter,
      printer: Variable => String
  ): String = {
    val baos = new ByteArrayOutputStream()
    val sb = new PrintStream(baos)
    val stats = section.source.stats.lift
    val totalStats = section.source.stats.length
    section.section.statements.zip(section.source.stats).zipWithIndex.foreach {
      case ((statement, tree), statementIndex) =>
        val pos = tree.pos
        val leadingBlankLines = stats(statementIndex - 1) match {
          case None =>
            0
          case Some(previousStatement) =>
            tree.pos.startLine - previousStatement.pos.endLine
        }
        sb.append("\n" * leadingBlankLines)
        val endOfLinePosition =
          Position.Range(pos.input, pos.startLine, pos.startColumn, pos.endLine, Int.MaxValue)
        sb.append(endOfLinePosition.text)
        if (statement.out.nonEmpty) {
          sb.append("\n")
          appendFreshMultiline(sb, statement.out)
        }
        val N = statement.binders.length
        statement.binders.zipWithIndex.foreach {
          case (binder, i) =>
            section.mod match {
              case Modifier.Fail =>
                sb.append('\n')
                binder.value match {
                  case CompileResult.TypecheckedOK(_, tpe, tpos) =>
                    reporter.error(
                      tpos.toMeta(section),
                      s"Expected compile error but statement type-checked successfully"
                    )
                    appendMultiline(sb, tpe)
                  case CompileResult.ParseError(msg, tpos) =>
                    appendFreshMultiline(sb, tpos.formatMessage(section, msg))
                  case CompileResult.TypeError(msg, tpos) =>
                    appendFreshMultiline(sb, tpos.formatMessage(section, msg))
                  case _ =>
                    val obtained = pprint.PPrinter.BlackWhite.apply(binder).toString()
                    throw new IllegalArgumentException(
                      s"Expected Macros.CompileResult." +
                        s"Obtained $obtained"
                    )
                }
              case Modifier.Default | Modifier.Passthrough =>
                val variable = new mdoc.Variable(
                  binder.name,
                  binder.tpe.render,
                  binder.value,
                  i,
                  N,
                  statementIndex,
                  totalStats
                )
                sb.append(printer(variable))
              case Modifier.Crash =>
                throw new IllegalArgumentException(Modifier.Crash.toString)
              case c @ (Modifier.Str(_, _) | Modifier.Silent) =>
                throw new IllegalArgumentException(c.toString)
            }
        }
    }
    baos.toString.trim
  }

}
