package mdoc.internal.worksheets

import mdoc.document.RangePosition
import mdoc.document.Statement
import mdoc.internal.cli.Context
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.internal.document.Printing
import mdoc.internal.io.StoreReporter
import mdoc.internal.markdown.Instrumenter
import mdoc.internal.markdown.MarkdownBuilder
import mdoc.internal.markdown.Modifier
import mdoc.internal.markdown.SectionInput
import mdoc.internal.pos.PositionSyntax._
import mdoc.{interfaces => i}

import java.{util => ju}
import scala.meta._

class WorksheetProvider(settings: Settings) {

  private val reporter = new StoreReporter()

  // The smallest column width that worksheet values will use for rendering
  // worksheet decorations.
  private val minimumMargin = 20

  def evaluateWorksheet(
      input: Input.VirtualFile,
      ctx: Context,
      modifier: Option[Modifier]
  ): EvaluatedWorksheet = {
    val sectionInput = SectionInput(input, modifier.getOrElse(Modifier.Default()), ctx)
    val sectionInputs = List(sectionInput)
    val file = InputFile.fromRelativeFilename(input.path, settings)
    val instrumented = Instrumenter.instrument(file, sectionInputs, settings, reporter)
    val compiler = ctx.compiler(instrumented)
    val rendered =
      MarkdownBuilder.buildDocument(compiler, reporter, sectionInputs, instrumented, input.path)
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
        .asJava,
      instrumented.fileImports.map(_.toInterface).asJava,
      instrumented.scalacOptionImports.map(_.value).asJava,
      compiler.classpathEntries.asJava,
      instrumented.dependencies.toSeq.asJava,
      instrumented.repositories.toSeq.asJava
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
          .append(binder.tpeString)
          .append(" = ")
        Printing.print(binder.stringValue, out, settings.screenWidth, settings.screenHeight)
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
                .append(": ")
                .append(binder.tpeString)
                .append(" = ")
            }
            if (isSingle)
              out
                .append(": ")
                .append(binder.tpeString)
                .append(" = ")

            Printing.printOneLine(binder.stringValue, out, width = margin - out.length)
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
