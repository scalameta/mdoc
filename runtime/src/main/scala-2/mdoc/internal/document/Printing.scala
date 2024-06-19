package mdoc.internal.document

import fansi.Str
import pprint.PPrinter
import pprint.PPrinter.BlackWhite
import pprint.TPrintColors

import Compat.TPrint

object Printing {

  def stringValue[T](value: T) = PPrinter.BlackWhite.apply(value)
  def typeString[T](tprint: TPrint[T]): String = tprint.render(TPrintColors.BlackWhite).render
  def print[T](value: T, out: StringBuilder, width: Int, height: Int) = {
    BlackWhite
      .tokenize(value, width, height)
      .foreach(text => out.appendAll(text.getChars))
  }

  def printOneLine(value: Str, out: StringBuilder, width: Int) = {
    out.appendAll(value.toString().replace("\n", " ").replaceAll("\\s+", " ")).take(width + 1)
  }

}
