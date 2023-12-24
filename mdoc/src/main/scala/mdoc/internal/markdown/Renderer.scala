package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.regex.Pattern
import mdoc.Reporter
import mdoc.Variable
import mdoc.document.CompileResult
import mdoc.document.CrashResult
import mdoc.document.CrashResult.Crashed
import mdoc.document.RangePosition
import mdoc.internal.document.FailSection
import mdoc.internal.document.MdocExceptions
import mdoc.internal.document.Printing
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance
import scala.meta._
import scala.meta.inputs.Position
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.internal.cli.Context

object Renderer {

  def render(
      file: InputFile,
      sections: List[Input],
      compiler: MarkdownCompiler,
      settings: Settings,
      reporter: Reporter,
      filename: String,
      printer: Variable => String,
      context: Context
  ): String = {
    val inputs =
      sections.map(s => SectionInput(s, Modifier.Default(), context))
    val instrumented = Instrumenter.instrument(file, inputs, settings, reporter)
    val doc =
      MarkdownBuilder.buildDocument(
        compiler,
        reporter,
        inputs,
        instrumented,
        filename
      )
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

  @deprecated("this method will be removed", "2020-06-01")
  def appendMultiline(sb: PrintStream, string: String, N: Int): Unit = {
    sb.appendMultiline(string, N)
  }

  def appendFreshMultiline(sb: PrintStream, string: String): Unit = {
    val N = string.length - (if (string.endsWith("\n")) 1 else 0)
    sb.append("// ")
    sb.appendMultiline(string, N)
  }

  // Beneath each binding statement, we insert the evaluated variable, e.g., `x: Int = 1`
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
    if (section.mod.isFailOrWarn) {
      sb.print(section.input.text)
    }
    section.section.statements.zip(section.source.stats).zipWithIndex.foreach {
      case ((statement, tree), statementIndex) =>
        val pos = tree.pos
        // for each statement, we need to manage:
        //   1. the leading trivia: whitespace, newlines
        //   2. a footer: empty until filled on the last statement in the section
        //   3. and, internally, the trailing single-line comments of the previous statement
        val (leading, footer) = stats(statementIndex - 1) match {
          case None =>
            (Position.Range(input, 0, pos.start).text, "")
          case Some(previousStatement) =>
            // for each statement, we need to:
            //   1. check the previous statement in order to find where the current statement begins
            //   2. re-split the input, as a workaround to missing comments in 'stats' and `Position.Range`
            //     a. use the stored comment-less variables to re-split the input
            //     b. split again on the newline to collect a comment, the trivia, and maybe a footer
            val escapedPrevStmt = Pattern.quote(previousStatement.toString())
            val prevWithTrivia = section.source.pos.text.split(escapedPrevStmt, 2).toList
            val (prevTrailingSingleLineComment, leadingTrivia, footerTrivia) =
              if (prevWithTrivia.length == 2) {
                val a2 = prevWithTrivia(1).split(Pattern.quote("\n"), 2)
                val prevComment = a2(0)
                val withTrivia = a2(1).split(Pattern.quote(tree.pos.text), 2)
                val (trivia, foot) = {
                  // ensure the last statement includes the footer if present
                  if ((withTrivia.length == 2) && (statementIndex != (totalStats - 1))) (withTrivia(0), "")
                  else if (withTrivia.length == 2) (withTrivia(0), withTrivia(1).dropWhile(_ != '\n'))
                  else (tree.pos.text, "")
                }
                (prevComment, trivia, foot)
              }
              else ("","","")
            val lead =
              // if no trailing single-line comments, then we can use the established `Position.Range` system
              if (prevWithTrivia.length == 0) Position.Range(input, previousStatement.pos.end, pos.start).text
              else "\n" + leadingTrivia
            (lead, footerTrivia)
        }
        if (!section.mod.isFailOrWarn) {
          sb.append(leading )
        }
        val endOfLinePosition =
          Position.Range(pos.input, pos.startLine, pos.startColumn, pos.endLine, Int.MaxValue)
        if (!section.mod.isFailOrWarn) {
          sb.append(endOfLinePosition.text)
        }
        if (statement.out.nonEmpty) {
          sb.append("\n")
          appendFreshMultiline(sb, statement.out)
        }
        val N = statement.binders.length
        statement.binders.zipWithIndex.foreach { case (binder, i) =>
          section.mod match {
            case Modifier.Fail() | Modifier.Warn() =>
              sb.append('\n')
              binder.value match {
                case FailSection(instrumented, startLine, startColumn, endLine, endColumn) =>
                  val input = Input.String(instrumented)
                  val edit =
                    TokenEditDistance.fromInputs(doc.sections.map(_.source.pos.input), input)
                  val compiled = compiler.fail(edit, input, section.source.pos)
                  val tpos = new RangePosition(startLine, startColumn, endLine, endColumn)
                  val pos = tpos.toMeta(section)
                  if (section.mod.isWarn && compiler.hasErrors) {
                    reporter.error(
                      pos,
                      s"Expected compile warnings but program failed to compile"
                    )
                  } else if (section.mod.isWarn && !compiler.hasWarnings) {
                    reporter.error(
                      pos,
                      s"Expected compile warnings but program compiled successfully without warnings"
                    )
                  } else if (section.mod.isFail && !compiler.hasErrors) {
                    reporter.error(
                      pos,
                      s"Expected compile errors but program compiled successfully without errors"
                    )
                  }
                  appendFreshMultiline(sb, compiled)
                case _ =>
                  val obtained = Printing.stringValue(binder.value)
                  throw new IllegalArgumentException(
                    s"Expected FailSection. Obtained $obtained"
                  )
              }
            case _ =>
              val pos = binder.pos.toMeta(section)
              val variable = new mdoc.Variable(
                binder.name,
                Printing.typeString(binder.tpe),
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
        sb.append(footer)
    }
    baos.toString.trim
  }

}
