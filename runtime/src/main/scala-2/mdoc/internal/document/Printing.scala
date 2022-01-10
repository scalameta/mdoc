package mdoc.internal.document

import pprint.TPrintColors
import pprint.PPrinter.BlackWhite
import pprint.PPrinter
import Compat.TPrint

object Printing {

  def stringValue[T](value: T) = PPrinter.BlackWhite.apply(value)
  def typeString[T](tprint: TPrint[T]): String = tprint.render(TPrintColors.BlackWhite).render
  def print[T](value: T, out: StringBuilder, width: Int, height: Int) = {
    BlackWhite
      .tokenize(value, width, height)
      .foreach(text => out.appendAll(text.getChars))
  }

  def printOneLine[T](value: T, out: StringBuilder, width: Int) = {
    val chunk = BlackWhite
      .tokenize(value, width)
      .map(_.getChars)
      .filterNot(_.iterator.forall(_.isWhitespace))
      .flatMap(_.iterator)
      .filter {
        case '\n' => false
        case _ => true
      }
    out.appendAll(chunk)

  }
}
