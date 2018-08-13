package vork.runtime

import scala.language.experimental.macros
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import pprint.PPrinter
import scala.collection.mutable.ArrayBuffer
import pprint.TPrint
import pprint.TPrintColors
import sourcecode.Text

final class Binder[T](val value: T, val name: String, val tpe: TPrint[T]) {
  override def toString: String = {
    val valueString = PPrinter.BlackWhite.apply(value)
    val tpeString = tpe.render(TPrintColors.BlackWhite)
    s"""Binder($valueString, "$name", "$tpeString")"""
  }
}
object Binder {
  def generate[A](e: Text[A])(implicit tprint: TPrint[A]): Binder[A] =
    new Binder(e.value, e.source, tprint)
}
case class Statement(binders: List[Binder[_]], out: String)
case class Section(statements: List[Statement]) {
  def isError: Boolean = statements.exists(_.isInstanceOf[Macros.CompileError])
}
case class Document(sections: List[Section])
object Document {
  val empty: Document = Document(Nil)
}

trait DocumentBuilder {
  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]
  private val myOut = new ByteArrayOutputStream()
  private var first = true

  object $doc {
    def binder[A](e: Text[A])(implicit tprint: TPrint[A]): A = {
      myBinders.append(Binder.generate(e))
      e.value
    }

    def statement[T](e: => T): T = {
      val out = myOut.toString()
      myOut.reset()
      myStatements.append(Statement(myBinders.toList, out))
      myBinders.clear()
      e
    }

    def section[T](e: => T): T = {
      if (first) {
        // HACK(olafur) we skip the leading $doc.section right after app().
        first = false
      } else {
        mySections.append(Section(myStatements.toList))
        myStatements.clear()
      }
      e
    }

    def build(): Document = {
      val out = new PrintStream(myOut)
      Console.withOut(out) {
        Console.withErr(out) {
          app()
        }
      }
      val document = Document(mySections.toList)
      mySections.clear()
      document
    }
  }

  def app(): Unit

}
