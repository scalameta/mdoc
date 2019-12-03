package mdoc.internal.worksheets
import mdoc.interfaces.RangePosition

case class EvaluatedWorksheetStatement(
    val position: RangePosition,
    val summary: String,
    val details: String,
    val isSummaryComplete: Boolean
) extends mdoc.interfaces.EvaluatedWorksheetStatement
