package vork.document

import pprint.PPrinter
import pprint.TPrint
import pprint.TPrintColors
import sourcecode.Text

final case class Document(sections: List[Section])

object Document {
  val empty: Document = Document(Nil)
}

final case class Section(statements: List[Statement])

final case class Statement(binders: List[Binder[_]], out: String)

final class Binder[T](val value: T, val name: String, val tpe: TPrint[T], pos: RangePosition) {
  override def toString: String = {
    val valueString = PPrinter.BlackWhite.apply(value)
    val tpeString = tpe.render(TPrintColors.BlackWhite)
    s"""Binder($valueString, "$name", "$tpeString")"""
  }
}
object Binder {
  def generate[A](e: Text[A], pos: RangePosition)(implicit tprint: TPrint[A]): Binder[A] =
    new Binder(e.value, e.source, tprint, pos: RangePosition)
}
