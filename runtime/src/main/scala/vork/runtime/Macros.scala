package vork.runtime

import scala.language.experimental.macros

import scala.reflect.macros.ParseException
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox

object Macros {
  sealed trait CompileResult
  final case object TypecheckedOK extends CompileResult
  sealed trait CompileError extends CompileResult
  final case class TypeError(msg: String) extends CompileError
  final case class ParseError(msg: String) extends CompileError

  def fail(code: String): CompileResult = macro failImpl
  def failImpl(c: blackbox.Context)(code: c.Tree): c.Tree = {
    import c.universe._
    val Literal(Constant(string: String)) = code

    try {
      c.typecheck(c.parse(string))
      reify { TypecheckedOK }.tree
    } catch {
      case e: ParseException =>
        val msg = c.literal(e.getMessage)
        reify { ParseError(msg.splice) }.tree
      case e: TypecheckException =>
        val msg = c.literal(e.getMessage)
        reify { TypeError(msg.splice) }.tree
    }
  }

}
