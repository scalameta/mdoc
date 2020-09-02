package mdoc.document

import pprint.PPrinter
import pprint.TPrint
import pprint.TPrintColors
import sourcecode.Text

final class Binder[T](val value: T, val name: String, val tpe: TPrint[T], val pos: RangePosition) {
  override def toString: String = {
    val valueString = PPrinter.BlackWhite.apply(value)
    val tpeString = tpe.render(TPrintColors.BlackWhite)
    s"""Binder($valueString, "$name", "$tpeString")"""
  }

  def tpeString = tpe.render(TPrintColors.BlackWhite)
}
object Binder {
  def generate[A](e: Text[A], pos: RangePosition)(implicit tprint: TPrint[A]): Binder[A] =
    new Binder(e.value, e.source, tprint, pos: RangePosition)
}
