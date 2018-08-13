package vork.runtime

import scala.language.experimental.macros
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import pprint.PPrinter
import scala.collection.mutable.ArrayBuffer
import pprint.TPrint
import pprint.TPrintColors
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import sourcecode.Text

final class PositionedException(
    val section: Int,
    val pos: RangePosition,
    val cause: Throwable
) extends Exception(pos.toString, cause)
    with NoStackTrace
final class RangePosition(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
) {
  def isEmpty: Boolean =
    startLine == -1 &&
      startColumn == -1 &&
      endLine == -1 &&
      endColumn == -1
  override def toString: String = {
    val end =
      if (startLine == endLine && startColumn == endColumn) ""
      else s"-$endLine:$endColumn"
    s"$startLine:$startColumn$end"
  }
}
object RangePosition {
  def empty: RangePosition = new RangePosition(-1, -1, -1, -1)
}
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
  private var sectionCount = 0
  private var lastPosition = RangePosition.empty

  object $doc {
    def binder[A](e: Text[A])(
        implicit tprint: TPrint[A]
    ): A = {
      binder(e, -1, -1, -1, -1)
    }
    def binder[A](e: Text[A], startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)(
        implicit tprint: TPrint[A]
    ): A = {
      val pos = new RangePosition(startLine, startColumn, endLine, endColumn)
      lastPosition = pos
      myBinders.append(Binder.generate(e, pos))
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
      sectionCount += 1
      if (first) {
        first = false
      } else {
        mySections.append(Section(myStatements.toList))
        myStatements.clear()
      }
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
            section { () }
          }
        }
      } catch {
        case NonFatal(e) =>
          val stacktrace = e.getStackTrace.takeWhile(!_.getClassName.startsWith("vork"))
          e.setStackTrace(stacktrace)
          throw new PositionedException(sectionCount, lastPosition, e)
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
