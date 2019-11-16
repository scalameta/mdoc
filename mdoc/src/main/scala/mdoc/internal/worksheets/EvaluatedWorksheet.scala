package mdoc.internal.worksheets

import java.{util => ju}
import mdoc.{interfaces => i}

case class EvaluatedWorksheet(
    val diagnostics: ju.List[i.Diagnostic],
    val statements: ju.List[i.EvaluatedWorksheetStatement]
) extends mdoc.interfaces.EvaluatedWorksheet
