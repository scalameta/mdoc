package vork.markdown.processors

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.collection.mutable.ArrayBuffer
import org.scalatest.FunSuite
import pprint.TPrint
import sourcecode.Text

class Binder[T](val value: T, name: String, val tpe: pprint.TPrint[T]) {
  override def toString: String = s"""Binder($value, "$name", "${tpe.render}")"""
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

class InstrumentedApp extends FunSuite with DocumentBuilder {

  val document = build()
  pprint.log(document, height = 1000, width = 70)
//  vork.markdown.processors.InstrumentedApp:78 document: Document(
//  List(
//    Section(
//      List(
//        Statement(List(Binder(List(1), "x", "List[Int]")), "", ""),
//        Statement(List(Binder(List(2), "y", "List[Int]")), "", ""),
//        Statement(
//          List(Binder(List(3), "x", "List[Int]")),
//          """x has length 1
//          """,
//          ""
//        )
//      )
//    ),
//    Section(
//      List(
//        Statement(
//          List(Binder(List(User(John,3)), "y", "List[User]")),
//          "",
//          """y users are List(User(John,3))
//          """
//        ),
//        Statement(List(), "", ""),
//        Statement(
//          List(Binder(List(User(3,John)), "y", "List[User]")),
//          "",
//          ""
//        ),
//        Statement(
//          List(
//            Binder(1, "x", "Int"),
//            Binder(2, "y", "Int"),
//            Binder(3, "z", "Int")
//          ),
//          "",
//          ""
//        )
//      )
//    )
//  )
//  )

  override def app(): Unit = {
    val x = List(1); binder(x);
    statement {
      val y = x.map(_ + 1); binder(y)
      statement {
        val x = y.map(_ + 1); binder(x)
        println(s"x has length " + x.length)
        statement {
          case class User(name: String, age: Int); // defined class User
          section {
            val y = x.map(i => User("John", i)); binder(y)
            System.err.println(s"y users are " + y)
            statement {
              case class User(age: Int, name: String); // defined class User
              statement {
                val y = x.map(i => User(i, "John")); binder(y)
                statement {
                  val List(x, y, z) = List(1, 2, 3); binder(x); binder(y); binder(z)
                  statement {
                    section {
                      ()
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

}
