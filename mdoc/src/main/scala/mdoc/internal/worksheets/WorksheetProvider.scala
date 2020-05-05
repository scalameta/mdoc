package mdoc.internal.worksheets

import scala.meta._
import mdoc.internal.cli.Context
import scala.collection.JavaConverters._
import mdoc.internal.markdown.SectionInput
import mdoc.internal.markdown.Modifier
import mdoc.internal.markdown.Instrumenter
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.document.Statement
import mdoc.document.RangePosition
import mdoc.internal.cli.Settings
import pprint.TPrintColors
import pprint.PPrinter.BlackWhite
import mdoc.internal.io.StoreReporter
import mdoc.{interfaces => i}
import mdoc.internal.markdown.MdocDialect

class WorksheetProvider(settings: Settings) {

  private val reporter = new StoreReporter()

  // The smallest column width that worksheet values will use for rendering
  // worksheet decorations.
  private val minimumMargin = 20

  def evaluateWorksheet(
      input: Input.VirtualFile,
      ctx: Context
  ): EvaluatedWorksheet = {
    val source = MdocDialect.scala(input).parse[Source].getOrElse(Source(Nil))
    val sectionInput = SectionInput(
      input,
      source,
      Modifier.Default()
    )
    val sectionInputs = List(sectionInput)
    val instrumented = Instrumenter.instrument(sectionInputs, reporter)
    val rendered = MarkdownCompiler.buildDocument(
      ctx.compiler,
      reporter,
      sectionInputs,
      instrumented.source,
      input.path
    )

    val decorations = for {
      section <- rendered.sections.iterator
      statement <- section.section.statements
    } yield renderDecoration(statement)

    EvaluatedWorksheet(
      reporter.diagnostics.map(d => d: i.Diagnostic).toSeq.asJava,
      decorations
        .filterNot(_.summary.isEmpty)
        .map(d => d: i.EvaluatedWorksheetStatement)
        .toList
        .asJava
    )
  }

  private def renderDecoration(statement: Statement): EvaluatedWorksheetStatement = {
    val pos = statement.position
    val range = new RangePosition(
      pos.startLine,
      pos.startColumn,
      pos.endLine,
      pos.endColumn
    )
    val margin = math.max(
      minimumMargin,
      settings.screenWidth - statement.position.endColumn
    )
    val isEmptyValue = isUnitType(statement) || statement.binders.isEmpty
    val renderSummaryResult =
      renderSummary(statement, margin, isEmptyValue)
    val details = renderDetails(statement, isEmptyValue)
    EvaluatedWorksheetStatement(
      range,
      renderSummaryResult.summary,
      details,
      renderSummaryResult.isSummaryComplete
    )
  }

  private def renderDetails(
      statement: Statement,
      isEmptyValue: Boolean
  ): String = {
    val out = new StringBuilder()
    if (!isEmptyValue) {
      statement.binders.iterator.foreach { binder =>
        out
          .append(if (out.nonEmpty) "\n" else "")
          .append(binder.name)
          .append(": ")
          .append(binder.tpe.render(TPrintColors.BlackWhite))
          .append(" = ")
        BlackWhite
          .tokenize(binder.value, width = settings.screenWidth, height = settings.screenHeight)
          .foreach(text => out.appendAll(text.getChars))
      }
    }
    statement.out.linesIterator.foreach { line =>
      out
        .append(if (out.nonEmpty) "\n" else "")
        .append("// ")
        .append(line)
    }
    out.toString()
  }

  private def renderSummary(
      statement: Statement,
      margin: Int,
      isEmptyValue: Boolean
  ): RenderSummaryResult = {
    val out = new StringBuilder()
    val isOverMargin =
      if (isEmptyValue) {
        if (!statement.out.isEmpty()) {
          val lines = statement.out.linesIterator
          out.append(lines.next())
          lines.hasNext || out.length > margin
        } else
          false
      } else {
        val isSingle = statement.binders.lengthCompare(1) == 0
        statement.binders.iterator.foldLeft(false) {
          case (true, _) => true
          case (false, binder) =>
            if (!isSingle) {
              out
                .append(if (out.isEmpty) "" else ", ")
                .append(binder.name)
                .append("=")
            }
            val chunk = BlackWhite
              .tokenize(binder.value, width = margin - out.length)
              .map(_.getChars)
              .filterNot(_.iterator.forall(_.isWhitespace))
              .flatMap(_.iterator)
              .filter {
                case '\n' => false
                case _ => true
              }
            out.appendAll(chunk)
            out.length > margin
        }
      }
    RenderSummaryResult(out.result().take(margin), isSummaryComplete = !isOverMargin)
  }

  private def isUnitType(statement: Statement): Boolean = {
    statement.binders match {
      case head :: Nil => () == head.value
      case _ => false
    }

  }
}

case class RenderSummaryResult(summary: String, isSummaryComplete: Boolean)
