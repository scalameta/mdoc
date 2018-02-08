package vork.runtime

import scala.language.experimental.macros

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.collection.mutable.ArrayBuffer
import pprint.TPrint
import sourcecode.Text

class Binder[T](val value: T, val name: String, val tpe: pprint.TPrint[T]) {
  override def toString: String =
    s"""Binder(${pprint.PPrinter.BlackWhite.apply(value)}, "$name", "${tpe.render}")"""
}
object Binder {
  def generate[A](e: Text[A])(implicit tprint: pprint.TPrint[A]): Binder[A] =
    new Binder(e.value, e.source, tprint)
}

case class Statement(binders: List[Binder[_]], out: String)
case class Section(statements: List[Statement]) {
  def isError: Boolean = statements.exists(_.isInstanceOf[Macros.CompileError])
}
case class Document(sections: List[Section])

trait DocumentBuilder {
  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]
  private val myOut = new ByteArrayOutputStream()

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
      mySections.append(Section(myStatements.toList))
      myStatements.clear()
      e
    }

    def build(): Document = {
      val backupStdout = System.out
      val backupStderr = System.err
      try {
        val out = new PrintStream(myOut)
        System.setOut(out)
        System.setErr(out)
        Console.withOut(out) {
          Console.withErr(out) {
            app()
          }
        }
      } finally {
        System.setOut(backupStdout)
        System.setErr(backupStderr)
      }
      val document = Document(mySections.toList)
      mySections.clear()
      document
    }
  }


  def app(): Unit

}

