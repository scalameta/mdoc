package vork.document

sealed trait CompileResult
object CompileResult {
  final case class TypecheckedOK(code: String, tpe: String, pos: RangePosition)
      extends CompileResult
  sealed trait CompileError extends CompileResult
  final case class TypeError(message: String, pos: RangePosition) extends CompileError
  final case class ParseError(message: String, pos: RangePosition) extends CompileError
}
