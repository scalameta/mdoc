package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import mdoc.Reporter
import mdoc.Variable
import mdoc.document.CompileResult
import mdoc.document.CrashResult
import mdoc.document.CrashResult.Crashed
import mdoc.document.RangePosition
import mdoc.internal.document.FailSection
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
    val sbt1WithLiteralTypes = dialects.Sbt1.copy(allowLiteralTypes = true)
    val inputs =
      sections.map(
        s => SectionInput(s, sbt1WithLiteralTypes(s).parse[Source].get, Modifier.Default())
      )
    val instrumented = Instrumenter.instrument(inputs)
    val doc =
      MarkdownCompiler.buildDocument(compiler, reporter, inputs, instrumented, filename)
    doc.sections
      .map(s => s"""```scala
                   |${Renderer.renderEvaluatedSection(doc, s, reporter, printer, compiler)}
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
    ps.println(section.source.pos.text)
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
      printer: Variable => String,
      compiler: MarkdownCompiler
  ): String = {
    val baos = new ByteArrayOutputStream()
    val sb = new PrintStream(baos)
    val stats = section.source.stats.lift
    val input = section.source.pos.input
    val totalStats = section.source.stats.length
    if (section.mod.isFail) {
      sb.print(section.input.text)
    }
    section.section.statements.zip(section.source.stats).zipWithIndex.foreach {
      case ((statement, tree), statementIndex) =>
        val pos = tree.pos
        val leadingStart = stats(statementIndex - 1) match {
          case None =>
            0
          case Some(previousStatement) =>
            previousStatement.pos.end
        }
        val leadingTrivia = Position.Range(input, leadingStart, pos.start)
        if (!section.mod.isFail) {
          sb.append(leadingTrivia.text)
        }
        val endOfLinePosition =
          Position.Range(pos.input, pos.startLine, pos.startColumn, pos.endLine, Int.MaxValue)
        if (!section.mod.isFail) {
          sb.append(endOfLinePosition.text)
        }
        if (statement.out.nonEmpty) {
          sb.append("\n")
          appendFreshMultiline(sb, statement.out)
        }
        val N = statement.binders.length
        statement.binders.zipWithIndex.foreach {
          case (binder, i) =>
            section.mod match {
              case Modifier.Fail() =>
                sb.append('\n')
                binder.value match {
                  case FailSection(instrumented, startLine, startColumn, endLine, endColumn) =>
                    val compiled = compiler.fail(
                      doc.sections.map(_.source),
                      Input.String(instrumented),
                      section.source.pos
                    )
                    if (compiled.isEmpty) {
                      val tpos = new RangePosition(startLine, startColumn, endLine, endColumn)
                      reporter.error(
                        tpos.toMeta(section),
                        s"Expected compile error but statement typechecked successfully"
                      )
                    }
                    appendFreshMultiline(sb, compiled)
                  case _ =>
                    val obtained = pprint.PPrinter.BlackWhite.apply(binder).toString()
                    throw new IllegalArgumentException(
                      s"Expected FailSection. Obtained $obtained"
                    )
                }
              case _ =>
                val pos = binder.pos.toMeta(section)
                val variable = new mdoc.Variable(
                  binder.name,
                  binder.tpe.render,
                  binder.value,
                  pos,
                  i,
                  N,
                  statementIndex,
                  totalStats,
                  section.mod
                )
                sb.append(printer(variable))
            }
        }
    }
    baos.toString.trim
  }

}
