package vork.runtime

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.collection.mutable.ArrayBuffer
import pprint.TPrint
import sourcecode.Text

class Binder[T](val value: T, name: String, val tpe: pprint.TPrint[T]) {
  override def toString: String =
    s"""Binder(${pprint.PPrinter.BlackWhite.apply(value)}, "$name", "${tpe.render}")"""
}
object Binder {
  def generate[A](e: Text[A])(implicit tprint: pprint.TPrint[A]): Binder[A] =
    new Binder(e.value, e.source, tprint)
}

case class Statement(expressions: List[Binder[_]], stdout: String, stderr: String)
case class Section(statements: List[Statement])
case class Document(sections: List[Section])

trait DocumentBuilder {
  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]
  private val myStdout = new ByteArrayOutputStream()
  private val myStderr = new ByteArrayOutputStream()

  def binder[A](e: Text[A])(implicit tprint: TPrint[A]): A = {
    myBinders.append(Binder.generate(e))
    e.value
  }

  def statement[T](e: => T): T = {
    val stdout = myStdout.toString()
    val stderr = myStderr.toString()
    myStdout.reset()
    myStderr.reset()
    myStatements.append(Statement(myBinders.toList, stdout, stderr))
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
      val out = new PrintStream(myStdout)
      val err = new PrintStream(myStderr)
      System.setOut(out)
      System.setErr(err)
      Console.withOut(out) {
        Console.withErr(err) {
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

  def app(): Unit
}
