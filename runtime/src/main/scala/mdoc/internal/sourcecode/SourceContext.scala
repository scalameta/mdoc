package mdoc.internal.sourcecode

case class SourceStatement[T](value: T, source: String)
object SourceStatement extends StatementMacro
