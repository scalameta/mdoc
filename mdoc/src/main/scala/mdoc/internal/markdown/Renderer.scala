package mdoc.internal.markdown

import mdoc.Reporter
import mdoc.Variable
import mdoc.document.CompileResult
import mdoc.document.CrashResult
import mdoc.document.CrashResult.Crashed
import mdoc.document.RangePosition
import mdoc.internal.cli.Context
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.internal.document.FailSection
import mdoc.internal.document.MdocExceptions
import mdoc.internal.document.Printing
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.inputs.Position
import mdoc.internal.markdown.Mod.Width
import mdoc.internal.markdown.Mod.Height

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
  def appendMultiline(sb: PrintStream, string: String, N: Int): Unit =
    sb.appendMultiline(string, N)

  def appendFreshMultiline(
      sb: PrintStream,
      string: String,
      heightOpt: Option[Int] = None,
      widthOpt: Option[Int] = None
  ): Unit = {
    if (heightOpt.isDefined || widthOpt.isDefined) {
      val lines = string.split("\n")
      val width = widthOpt.getOrElse(lines.map(_.length).max)
      val height = heightOpt.getOrElse(lines.length)

      val linesTruncatedToWidth = lines
        .map { line =>
          line.take(width) + (if (line.length > width) "..." else "")
        }

      val linesTruncatedToHeigth = linesTruncatedToWidth.take(
        height
      ) ++ (if (linesTruncatedToWidth.length > height) List("...") else List.empty[String])

      sb.append("// ")
      sb.appendMultiline(linesTruncatedToHeigth.mkString("\n"))
    } else {
      val N = string.length - (if (string.endsWith("\n")) 1 else 0)
      sb.append("// ")
      sb.appendMultiline(string, N)
    }
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
    if (section.mod.isFailOrWarn || section.section.statements.isEmpty) {
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
            val Array(prevTrailingSingleLineComment, leadingTrivia) =
              section.source.pos.text.substring(previousStatement.pos.end, pos.start).split("\n", 2)
            val foot =
              if (statementIndex != (totalStats - 1)) ""
              else
                section.source.pos.text
                  .substring(pos.end)
                  .split("\n")
                  .drop(1)
                  .mkString("\n", "\n", "")
            ("\n" + leadingTrivia, foot)
        }
        if (!section.mod.isFailOrWarn) {
          sb.append(leading)
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
                    TokenEditDistance.fromTrees(Seq(section.source.source), input)
                  val compiled = compiler.fail(edit, input, section.source.pos)
                  val tpos = new RangePosition(startLine, startColumn, endLine, endColumn)
                  val pos = tpos.toMeta(section)
                  val widthOpt = section.mod.mods.collectFirst { case Width(width) => width }
                  val heightOpt = section.mod.mods.collectFirst { case Height(height) => height }
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

                  appendFreshMultiline(sb, compiled, heightOpt, widthOpt)
                case _ =>
                  val obtained = binder.stringValue
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
