package vork.document

sealed trait CompileResult
object CompileResult {
  final case class TypecheckedOK(code: String, tpe: String) extends CompileResult
  sealed trait CompileError extends CompileResult
  final case class TypeError(message: String) extends CompileError
  final case class ParseError(message: String) extends CompileError
}
