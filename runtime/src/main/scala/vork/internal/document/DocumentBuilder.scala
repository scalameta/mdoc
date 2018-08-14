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
  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]
  private val myOut = new ByteArrayOutputStream()
  private var first = true
  private var sectionCount = 0
  private var lastPosition = RangePosition.empty

  object $doc {

    def position(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): RangePosition = {
      val pos = new RangePosition(startLine, startColumn, endLine, endColumn)
      lastPosition = pos
      pos
    }

    def binder[A](e: Text[A])(
        implicit tprint: TPrint[A]
    ): A = {
      binder(e, -1, -1, -1, -1)
    }

    def binder[A](e: Text[A], startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)(
        implicit tprint: TPrint[A]
    ): A = {
      val pos = position(startLine, startColumn, endLine, endColumn)
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

    def build(input: InstrumentedInput): Document = {
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
          VorkExceptions.trimStacktrace(e)
          throw new PositionedException(sectionCount, lastPosition, e)
      } finally {
        System.setOut(backupStdout)
        System.setErr(backupStderr)
      }
      val document = Document(input, mySections.toList)
      mySections.clear()
      document
    }
  }

  def app(): Unit

}
