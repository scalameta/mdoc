package vork.internal.document

import scala.language.experimental.macros
import scala.reflect.macros.ParseException
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox
import vork.document.CompileResult
import vork.document.CompileResult._

object Macros {

  def fail(code: String): CompileResult = macro failImpl
  def failImpl(c: blackbox.Context)(code: c.Tree): c.Tree = {
    import c.universe._
    val string = code match {
      case Literal(Constant(str: String)) => str
      case els =>
        c.abort(c.enclosingPosition, s"Expe" +
          s"cted string literal, obtained $els")
    }

    def lit(s: String) = c.literal(s)

    def formatPosition(message: String, pos: scala.reflect.api.Position): c.Expr[String] =
      lit(
        new StringBuilder()
          .append(message)
          .append("\n")
          .append(pos.lineContent)
          .append("\n")
          .append(" " * (pos.column - 1))
          .append("^")
          .toString
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
