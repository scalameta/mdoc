package mdoc.internal.document

case class FailSection(
    code: String,
    startLine: Int,
    startColumn: Int,
    endLine: Int,
    endColumn: Int
)
