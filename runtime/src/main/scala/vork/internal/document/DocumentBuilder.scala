package vork.internal.document

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import pprint.TPrint
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros
import scala.util.control.NonFatal
import sourcecode.Text
import vork.document._

trait DocumentBuilder {

  def app(): Unit

  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]
  private val myOut = new ByteArrayOutputStream()
  private val myPs = new PrintStream(myOut)
  private var lastPosition = RangePosition.empty
  final def println(a: Any): Unit = myPs.println(a)
  final def print(a: Any): Unit = myPs.print(a)
  final def printf(text: String, xs: Any*): Unit = myPs.printf(text.format(xs))

  object $doc {

    def position(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): RangePosition = {
      val pos = new RangePosition(startLine, startColumn, endLine, endColumn)
      lastPosition = pos
      pos
    }

    def binder[A](e: Text[A], startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)(
        implicit tprint: TPrint[A]
    ): A = {
      val pos = position(startLine, startColumn, endLine, endColumn)
      myBinders.append(Binder.generate(e, pos))
      e.value
    }

    def startStatement(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): Unit = {
      position(startLine, startColumn, endLine, endColumn)
      myBinders.clear()
      myOut.reset()
    }
    def endStatement(): Unit = {
      val out = myOut.toString()
      myStatements.append(Statement(myBinders.toList, out))
    }

    def startSection(): Unit = {
      myStatements.clear()
    }
    def endSection(): Unit = {
      mySections.append(Section(myStatements.toList))
    }

    def crash(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)(
        thunk: => Any
    ): Unit = {
      val pos = new RangePosition(startLine, startColumn, endLine, endColumn)
      val result =
        try {
          thunk
          CrashResult.Success(pos)
        } catch {
          case NonFatal(e) =>
            CrashResult.Crashed(e, pos)
        }
      myBinders.append(Binder.generate(result, pos))
    }

    def build(input: InstrumentedInput): Document = {
      try {
        app()
      } catch {
        case NonFatal(e) =>
          VorkExceptions.trimStacktrace(e)
          throw new PositionedException(mySections.length, lastPosition, e)
      }
      val document = Document(input, mySections.toList)
      mySections.clear()
      document
    }
  }

}
