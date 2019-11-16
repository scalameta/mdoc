package mdoc.internal.worksheets

import scala.meta._
import mdoc.internal.cli.Context
import scala.collection.JavaConverters._
import mdoc.internal.markdown.SectionInput
import scala.meta.parsers.Parsed.Success
import mdoc.internal.markdown.Modifier
import mdoc.internal.markdown.Instrumenter
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.document.Statement
import mdoc.document.RangePosition
import mdoc.internal.cli.Settings
import pprint.TPrintColors
import pprint.PPrinter.BlackWhite
import mdoc.internal.io.StoreReporter
import mdoc.interfaces.Diagnostic
import mdoc.{interfaces => i}
import mdoc.internal.markdown.MdocDialect

class WorksheetProvider(settings: Settings) {

  private val reporter = new StoreReporter()

  private val commentHeader = " // "
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
    val instrumented = Instrumenter.instrument(sectionInputs)
    val rendered = MarkdownCompiler.buildDocument(
      ctx.compiler,
      reporter,
      sectionInputs,
      instrumented,
      input.path
    )

    val decorations = for {
      section <- rendered.sections.iterator
      statement <- section.section.statements
    } yield renderDecoration(statement)

    EvaluatedWorksheet(
      reporter.diagnostics.map(d => d: i.Diagnostic).toSeq.asJava,
      decorations.toIterator
        .filterNot(_.summary == commentHeader)
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
    val contentText = renderContentText(statement, margin, isEmptyValue)
    val hoverMessage = renderHoverMessage(statement, margin, isEmptyValue)
    EvaluatedWorksheetStatement(range, contentText, hoverMessage)
  }

  private def renderHoverMessage(
      statement: Statement,
      margin: Int,
      isEmptyValue: Boolean
  ): String = {
    val out = new StringBuilder()
    if (!isEmptyValue) {
      statement.binders.iterator.foreach { binder =>
        out
          .append("\n")
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
      out.append("\n// ").append(line)
    }
    out.toString()
  }

  private def renderContentText(
      statement: Statement,
      margin: Int,
      isEmptyValue: Boolean
  ): String = {
    val out = new StringBuilder()
    out.append(commentHeader)
    if (isEmptyValue) {
      if (!statement.out.isEmpty()) {
        out.append(statement.out.linesIterator.next())
      }
    } else {
      val isSingle = statement.binders.lengthCompare(1) == 0
      statement.binders.iterator.zipWithIndex.foreach {
        case (binder, i) =>
          if (!isSingle) {
            out
              .append(if (i == 0) "" else ", ")
              .append(binder.name)
              .append("=")
          }
          val truncatedLine = BlackWhite
            .tokenize(binder.value, width = margin, height = settings.screenHeight)
            .map(_.getChars)
            .filterNot(_.iterator.forall(_.isWhitespace))
            .flatMap(_.iterator)
            .filter {
              case '\n' => false
              case _ => true
            }
            .take(margin)
          out.appendAll(truncatedLine)
      }
    }
    out.toString()
  }

  private def isUnitType(statement: Statement): Boolean = {
    statement.binders match {
      case head :: Nil => () == head.value
      case _ => false
    }

  }
}
