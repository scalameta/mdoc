package mdoc.internal.sourcecode

import scala.language.implicitConversions
import scala.quoted._
import scala.tasty.Reflection

trait StatementMacro {
  inline implicit def generate[T](v: => T): SourceStatement[T] = ${ Macros.text('v) }
  inline def apply[T](v: => T): SourceStatement[T] = ${ Macros.text('v) }
}

object Macros{

  def text[T: Type](v: Expr[T])(using ctx: QuoteContext): Expr[SourceStatement[T]] = {
    import ctx.tasty._
    val txt = v.unseal.pos.sourceCode
    '{SourceStatement[T]($v, ${Expr(txt)})}
  }
}