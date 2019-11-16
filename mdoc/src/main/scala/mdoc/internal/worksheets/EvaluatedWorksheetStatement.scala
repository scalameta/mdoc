package mdoc.internal.worksheets
import mdoc.interfaces.RangePosition

case class EvaluatedWorksheetStatement(
    val position: RangePosition,
    val summary: String,
    val details: String
) extends mdoc.interfaces.EvaluatedWorksheetStatement
