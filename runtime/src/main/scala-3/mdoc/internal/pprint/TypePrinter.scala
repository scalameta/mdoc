package mdoc.internal.pprint

import scala.language.implicitConversions
import scala.quoted._
import scala.quoted.runtime.impl.printers.SyntaxHighlight

trait TPrint[T] {
  def render: String
}

object TPrint {
  inline given default[T]: TPrint[T] = ${ TypePrinter.typeString[T] }
}

object TypePrinter {

  def typeString[T](using ctx: Quotes, tpe: Type[T]): Expr[TPrint[T]] = {
    import ctx.reflect._

    val valueType = TypeTree.of[T](using tpe).tpe.show(using Printer.TypeReprShortCode)

    '{ new TPrint[T] { def render: String = ${ Expr(valueType) } } }
  }
}
