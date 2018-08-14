package vork.internal.document

import scala.language.experimental.macros
import scala.reflect.macros.ParseException
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox
import vork.document.CompileResult
import vork.document.CompileResult._
import vork.document.RangePosition

object Macros {

  def fail(
      code: String,
      startLine: Int,
      startColumn: Int,
      endLine: Int,
      endColumn: Int
  ): CompileResult = macro failImpl

  def failImpl(c: blackbox.Context)(
      code: c.Expr[String],
      startLine: c.Expr[Int],
      startColumn: c.Expr[Int],
      endLine: c.Expr[Int],
      endColumn: c.Expr[Int]
  ): c.Tree = {
    import c.universe._
    val string = code.tree match {
      case Literal(Constant(str: String)) => str
      case els =>
        c.abort(
          c.enclosingPosition,
          s"Expe" +
            s"cted string literal, obtained $els"
        )
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
    def rangePos(pos: scala.reflect.api.Position): c.Expr[RangePosition] = {
      val line = c.literal(pos.line)
      val column = c.literal(pos.column)
      reify {
        new RangePosition(line.splice, column.splice, line.splice, column.splice)
      }
    }

    try {
      val typechecked = c.typecheck(c.parse(string))
      reify {
        TypecheckedOK(
          lit(string).splice,
          lit(typechecked.tpe.toString).splice,
          new RangePosition(startLine.splice, startColumn.splice, endLine.splice, endColumn.splice)
        )
      }.tree
    } catch {
      case e: ParseException =>
        val msg = formatPosition(e.getMessage, e.pos)
        reify {
          ParseError(msg.splice, rangePos(e.pos).splice)
        }.tree
      case e: TypecheckException =>
        val msg = formatPosition(e.getMessage, e.pos)
        reify {
          TypeError(msg.splice, rangePos(e.pos).splice)
        }.tree
    }
  }

}
