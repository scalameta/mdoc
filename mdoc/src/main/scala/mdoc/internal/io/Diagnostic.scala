package mdoc.internal.io

import mdoc.interfaces.DiagnosticSeverity
import mdoc.interfaces.RangePosition

case class Diagnostic(
    val position: RangePosition,
    val message: String,
    val severity: DiagnosticSeverity
) extends mdoc.interfaces.Diagnostic
