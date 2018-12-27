package mdoc.internal.document

import scala.language.experimental.macros
import scala.reflect.macros.ParseException
import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox
import mdoc.document.CompileResult
import mdoc.document.CompileResult._
import mdoc.document.RangePosition

object Macros {
  case class Delay(
      code: String,
      startLine: Int,
      startColumn: Int,
      endLine: Int,
      endColumn: Int
  )
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

    def rangePos(pos: scala.reflect.api.Position): c.Expr[RangePosition] = {
      reify {
        val line = startLine.splice + c.literal(pos.line - 1).splice
        val column = startColumn.splice + c.literal(pos.column - 1).splice
        new RangePosition(line, column, line, column)
      }
    }

    def codePosition: c.Expr[RangePosition] = {
      reify {
        new RangePosition(startLine.splice, startColumn.splice, endLine.splice, endColumn.splice)
      }
    }

    try {
      val typechecked = c.typecheck(c.parse(string))
      reify {
        TypecheckedOK(
          c.literal(string).splice,
          c.literal(typechecked.tpe.toString).splice,
          codePosition.splice
        )
      }.tree
    } catch {
      case e: ParseException =>
        val msg = c.literal(e.getMessage)
        reify {
          ParseError(msg.splice, rangePos(e.pos).splice)
        }.tree
      case e: TypecheckException =>
        val msg = c.literal(e.getMessage)
        reify {
          TypeError(msg.splice, rangePos(e.pos).splice)
        }.tree
    }
  }

}
