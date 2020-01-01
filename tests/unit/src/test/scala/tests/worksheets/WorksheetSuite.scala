package tests.worksheets

import java.lang.StringBuilder
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import mdoc.interfaces.{DiagnosticSeverity, Mdoc}
import scala.meta.testkit.DiffAssertions
import scala.collection.JavaConverters._
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.internal.pos.PositionSyntax._
import java.{util => ju}

class WorksheetSuite extends FunSuite with BeforeAndAfterAll with DiffAssertions {
  var mdoc = ju.ServiceLoader
    .load(classOf[Mdoc], this.getClass().getClassLoader())
    .iterator()
    .next()
    .withScreenWidth(30)
    .withScreenHeight(5)
  override def afterAll(): Unit = {
    mdoc.shutdown()
  }

  def checkDiagnostics(
      name: String,
      original: String,
      expected: String
  ): Unit = {
    test(name) {
      val filename = name + ".scala"
      val worksheet = mdoc.evaluateWorksheet(filename, original)
      val input = Input.VirtualFile(name, original)
      val out = new StringBuilder()
      var i = 0
      val diagnostics =
        worksheet.diagnostics.asScala.filter(_.severity() == DiagnosticSeverity.Error)
      diagnostics.foreach { diag =>
        val p = Position.Range(
          input,
          diag.position().startLine(),
          diag.position().startColumn(),
          diag.position().endLine(),
          diag.position().endColumn()
        )
        val message =
          p.formatMessage(diag.severity().toString().toLowerCase(), diag.message())
        out.append(message).append("\n")
      }
      val obtained = out.toString()
      assertNoDiffOrPrintExpected(obtained, expected)
    }
  }

  def checkDecorations(
      name: String,
      original: String,
      expected: String
  ): Unit = {
    test(name) {
      val filename = name + ".scala"
      val worksheet = mdoc.evaluateWorksheet(filename, original)
      val statements = worksheet.statements().asScala.sortBy(_.position().startLine())
      val input = Input.VirtualFile(name, original)
      val out = new StringBuilder()
      var i = 0
      statements.foreach { stat =>
        val p = Position.Range(
          input,
          stat.position().startLine(),
          stat.position().startColumn(),
          stat.position().endLine(),
          stat.position().endColumn()
        )
        out
          .append(original, i, p.start)
          .append("<")
          .append(p.text)
          .append(">")
          .append(" // ")
          .append(stat.summary())
          .append(if (!stat.isSummaryComplete) "..." else "")
          .append("\n")
          .append(stat.details())
        i = p.end
      }
      val obtained = out.toString()
      assertNoDiffOrPrintExpected(obtained, expected)
    }
  }

  checkDecorations(
    "basic",
    """
      |val x = 1.to(4).toVector
      |""".stripMargin,
    """|
       |<val x = 1.to(4).toVector> // Vector(1, 2, 3, 4)
       |x: Vector[Int] = Vector(1, 2, 3, 4)
       |""".stripMargin
  )

  checkDecorations(
    "binders",
    """
      |val List(x, y) = List(1, 2)
      |""".stripMargin,
    """|
       |<val List(x, y) = List(1, 2)> // x=1, y=2
       |x: Int = 1
       |y: Int = 2
       |""".stripMargin
  )

  checkDecorations(
    "stream",
    """
      |Stream.from(10)
      |""".stripMargin,
    """|
       |<Stream.from(10)> // Stream(10,11,12,13,1...
       |res0: Stream[Int] = Stream(
       |  10,
       |  11,
       |  12,
       |...
       |""".stripMargin
  )

  checkDecorations(
    "stdout",
    """
      |println(1.to(3).mkString(";\n"))
      |""".stripMargin,
    """|
       |<println(1.to(3).mkString(";\n"))> // 1;...
       |// 1;
       |// 2;
       |// 3
       |""".stripMargin
  )

  checkDecorations(
    "stdout+value",
    """
      |val x = {
      |  println("hello")
      |  42
      |}
      |""".stripMargin,
    """|
       |<val x = {
       |  println("hello")
       |  42
       |}> // 42
       |x: Int = 42
       |// hello
       |""".stripMargin
  )

  checkDecorations(
    "multi-statements",
    """
      |val n = 10
      |println(n)
      |val m = n * 10
      |""".stripMargin,
    """|
       |<val n = 10> // 10
       |n: Int = 10
       |<println(n)> // 10
       |// 10
       |<val m = n * 10> // 100
       |m: Int = 100
       |""".stripMargin
  )

  checkDecorations(
    "imports",
    """import scala.concurrent.Future
      |val n = Future.successful(10)
      |""".stripMargin,
    """|import scala.concurrent.Future
       |<val n = Future.successful(10)> // Future(Success(10))
       |n: Future[Int] = Future(Success(10))
       |""".stripMargin
  )

  checkDecorations(
    "definition",
    """case class User(name: String)
      |val n = User("Susan")
      |""".stripMargin,
    """|case class User(name: String)
       |<val n = User("Susan")> // User("Susan")
       |n: User = User("Susan")
       |""".stripMargin
  )

  checkDiagnostics(
    "type-error",
    """
      |val filename: Int = "not found"
      |""".stripMargin,
    """|type-error:2:21: error: type mismatch;
       | found   : String("not found")
       | required: Int
       |val filename: Int = "not found"
       |                    ^^^^^^^^^^^
       |""".stripMargin
  )

  checkDiagnostics(
    "crash",
    """
      |def crash(msg: String) = throw new RuntimeException(msg)
      |val filename = "boom"
      |crash(filename)
      |""".stripMargin,
    """|crash:4:1: error: java.lang.RuntimeException: boom
       |	at repl.Session$App.crash(crash.scala:8)
       |	at repl.Session$App.<init>(crash.scala:14)
       |	at repl.Session$.app(crash.scala:3)
       |
       |crash(filename)
       |^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  checkDecorations(
    "partial-exception",
    """
      |val x = "foobar".stripSuffix("bar")
      |throw new RuntimeException("boom")
      |val y = "foobar".stripSuffix("bar")
      |""".stripMargin,
    """|
       |<val x = "foobar".stripSuffix("bar")> // "foo"
       |x: String = "foo"
       |""".stripMargin
  )

}
