package mdoc.document

sealed trait CompileResult
object CompileResult {

  final case class TypecheckedOK(code: String, tpe: String, pos: RangePosition)
      extends CompileResult

  sealed trait CompileError extends CompileResult

  /**
    * Compiler reported an error message during typechecking.
    * @param message the typechecking error message (without position formatting)
    * @param pos the range position inside the code fence
    */
  final case class TypeError(message: String, pos: RangePosition) extends CompileError

  /**
    * Compiler reported an error message during parsing.
    * @param message the syntax error message (without position formatting)
    * @param pos the range position inside the code fence
    */
  final case class ParseError(message: String, pos: RangePosition) extends CompileError

}
