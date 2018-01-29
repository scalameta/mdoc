package vork.markdown.processors

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

case class Statement(expressions: List[Binder[_]])
case class Section(statements: List[Statement])
case class Document(sections: List[Section])

trait DocumentBuilder {
  private val myBinders = ArrayBuffer.empty[Binder[_]]
  private val myStatements = ArrayBuffer.empty[Statement]
  private val mySections = ArrayBuffer.empty[Section]

  def binder[A](e: Text[A])(implicit tprint: TPrint[A]): A = {
    myBinders.append(Binder.generate(e))
    e.value
  }

  def statement[T](e: => T): T = {
    myStatements.append(Statement(myBinders.toList))
    myBinders.clear()
    e
  }

  def section[T](e: => T): T = {
    mySections.append(Section(myStatements.toList))
    myStatements.clear()
    e
  }

  def build(): Document = {
    app()
    val document = Document(mySections.toList)
    mySections.clear()
    document
  }

  def app(): Unit
}

class InstrumentedApp extends FunSuite with DocumentBuilder {

  val document = build()
  pprint.log(document, height = 1000, width = 70)
//  vork.markdown.processors.InstrumentedApp:55 document: Document(
//  List(
//    Section(
//      List(
//        Statement(List(Binder(List(1), "x", "List[Int]"))),
//        Statement(List(Binder(List(2), "y", "List[Int]"))),
//        Statement(List(Binder(List(3), "x", "List[Int]")))
//      )
//    ),
//    Section(
//      List(
//        Statement(List(Binder(List(User(John,3)), "y", "List[User]"))),
//        Statement(List()),
//        Statement(List(Binder(List(User(3,John)), "y", "List[User]"))),
//        Statement(
//          List(
//            Binder(1, "x", "Int"),
//            Binder(2, "y", "Int"),
//            Binder(3, "z", "Int")
//          )
//        )
//      )
//    )
//  )
//  )

  override def app(): Unit = {
    val x = List(1); binder(x)
    statement {
      val y = x.map(_ + 1); binder(y)
      statement {
        val x = y.map(_ + 1); binder(x)
        statement {
          case class User(name: String, age: Int); // defined class User
          section {
            val y = x.map(i => User("John", i)); binder(y)
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
