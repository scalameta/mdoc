package mdoc.document

import mdoc.internal.sourcecode.SourceStatement
import mdoc.internal.document.Printing
import mdoc.internal.document.Compat.TPrint

final class Binder[T](val value: T, val name: String, val tpe: TPrint[T], val pos: RangePosition) {
  override def toString: String = {
    val valueString = Printing.stringValue(value)
    s"""Binder($valueString, "$name", "$tpeString")"""
  }

  def tpeString = Printing.typeString(tpe).stripPrefix("<empty>.")
}
object Binder {
  def generate[A](e: SourceStatement[A], pos: RangePosition)(implicit
      tprint: TPrint[A]
  ): Binder[A] =
    new Binder(e.value, e.source, tprint, pos: RangePosition)
}
