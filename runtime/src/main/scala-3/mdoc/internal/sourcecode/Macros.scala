package mdoc.internal.sourcecode

import scala.language.implicitConversions
import scala.quoted._

trait StatementMacro {
  inline implicit def generate[T](v: => T): SourceStatement[T] = ${ Macros.text('v) }
  inline def apply[T](v: => T): SourceStatement[T] = ${ Macros.text('v) }
}

object Macros{

  def text[T: Type](v: Expr[T])(using ctx: Quotes): Expr[SourceStatement[T]] = {
    import ctx.reflect.{_, given}
    val txt =  Term.of(v).pos.sourceCode
    '{SourceStatement[T]($v, ${Expr(txt)})}
  }
}