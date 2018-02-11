package vork.runtime

import scala.language.experimental.macros

import scala.reflect.macros.ParseException
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox

object Macros {
  sealed trait CompileResult
  final case class TypecheckedOK(code: String, tpe: String) extends CompileResult
  sealed trait CompileError extends CompileResult
  final case class TypeError(msg: String) extends CompileError
  final case class ParseError(msg: String) extends CompileError

  def fail(code: String): CompileResult = macro failImpl
  def failImpl(c: blackbox.Context)(code: c.Tree): c.Tree = {
    import c.universe._
    val string = code match {
      case Literal(Constant(str: String)) => str
      case els =>
        c.abort(c.enclosingPosition, s"Expected string literal, obtained $els")
    }

    def lit(s: String) = c.literal(s)

    def formatPosition(message: String, pos: scala.reflect.api.Position): c.Expr[String] =
      c.literal(
        s"""$message
           |${pos.lineContent}
           |${(" " * (pos.column - 1)) + "^"}""".stripMargin
      )

    try {
      val typechecked = c.typecheck(c.parse(string))
      reify {
        TypecheckedOK(
          lit(string).splice,
          lit(typechecked.tpe.toString).splice
        )
      }.tree
    } catch {
      case e: ParseException =>
        val msg = formatPosition(e.getMessage, e.pos)
        reify { ParseError(msg.splice) }.tree
      case e: TypecheckException =>
        val msg = formatPosition(e.getMessage, e.pos)
        reify { TypeError(msg.splice) }.tree
    }
  }

}
