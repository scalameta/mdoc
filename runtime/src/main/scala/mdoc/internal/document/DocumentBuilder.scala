package mdoc.internal.document

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import mdoc.document._
import mdoc.internal.document.Compat.TPrint
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros
import scala.util.control.NonFatal
import mdoc.internal.sourcecode.SourceStatement

trait DocumentBuilder {

  def app(): Unit

  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]
  private val myOut = new ByteArrayOutputStream()
  private val myPs = new PrintStream(myOut)
  private var statementPosition = RangePosition.empty
  private var lastPosition = RangePosition.empty

  def print(obj: Any): Unit = {
    myPs.print(if (null == obj) "null" else obj.toString())
  }
  def println(): Unit = myPs.println()
  def println(x: Any): Unit = myPs.println(x)

  object $doc {

    def position(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): RangePosition = {
      val pos = new RangePosition(startLine, startColumn, endLine, endColumn)
      lastPosition = pos
      pos
    }

    def binder[A](
        e: SourceStatement[A],
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    )(implicit
        tprint: TPrint[A]
    ): A = {
      val pos = position(startLine, startColumn, endLine, endColumn)
      myBinders.append(Binder.generate(e, pos))
      e.value
    }

    def startStatement(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): Unit = {
      statementPosition = position(startLine, startColumn, endLine, endColumn)
      myBinders.clear()
      myOut.reset()
    }
    def endStatement(): Unit = {
      val out = myOut.toString()
      myStatements.append(Statement(myBinders.toList, out, statementPosition))
    }

    def startSection(): Unit = {
      myStatements.clear()
    }
    def endSection(): Unit = {
      mySections.append(Section(myStatements.toList))
    }

    def crash(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)(
        thunk: => Any
    )(implicit tprint: TPrint[CrashResult]): Unit = {
      val pos = new RangePosition(startLine, startColumn, endLine, endColumn)
      val result: CrashResult =
        try {
          thunk
          CrashResult.Success(pos)
        } catch {
          case MdocNonFatal(e) =>
            CrashResult.Crashed(e, pos)
        }
      // We can't generate macros in the same unit and the name of result will be "result" anyway
      myBinders.append(new Binder(result, "result", tprint, pos))
    }

    def build(input: InstrumentedInput): Document = {
      try {
        Console.withOut(myPs) {
          val oldOut = System.out
          try {
            System.setOut(myPs)
            Console.withErr(myPs) {
              val oldErr = System.err
              try {
                System.setErr(myPs)
                app()
              } finally {
                System.setErr(oldErr)
              }
            }
          } finally {
            System.setOut(oldOut)
          }
        }
      } catch {
        case MdocNonFatal(e) =>
          endStatement()
          endSection()
          MdocExceptions.trimStacktrace(e)
          throw new DocumentException(mySections.toList, lastPosition, e)
      }
      val document = Document(input, mySections.toList)
      mySections.clear()
      document
    }
  }

}
