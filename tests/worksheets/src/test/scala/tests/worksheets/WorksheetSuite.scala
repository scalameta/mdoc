package tests.worksheets

import mdoc.interfaces.DiagnosticSeverity
import mdoc.interfaces.Mdoc
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.CompatClassloader
import munit.TestOptions

import java.lang.StringBuilder
import java.{util => ju}
import java.nio.file.Paths

import scala.collection.JavaConverters._
import scala.meta.inputs.Input
import scala.meta.inputs.Position

import tests.BaseSuite
import tests.markdown.Compat

class WorksheetSuite extends BaseSuite {

  var mdoc = ju.ServiceLoader
    .load(classOf[Mdoc], this.getClass().getClassLoader())
    .iterator()
    .next()
    .withScreenWidth(30)
    .withScreenHeight(5)
    .withClasspath(
      CompatClassloader
        .getURLs(this.getClass().getClassLoader())
        .collect { case url if url.toString.contains("dotty-library") => Paths.get(url.toURI()) }
        .asJava
    )

  override def afterAll(): Unit = {
    mdoc.shutdown()
  }

  checkDecorations(
    "basic",
    """
      |val x = 1.to(4).toVector
      |""".stripMargin,
    """|
       |<val x = 1.to(4).toVector> // : Vector[Int] = Vect...
       |x: Vector[Int] = Vector(1, 2, 3, 4)
       |""".stripMargin
  )

  checkDecorations(
    "lazy",
    """
      |lazy val x = 0.0
      |val y = 1.0
      |""".stripMargin,
    """|lazy val x = 0.0
       |<val y = 1.0> // : Double = 1.0
       |y: Double = 1.0
       |""".stripMargin
  )

  checkDecorations(
    "binders",
    """
      |val List(x, y) = List(1, 2)
      |""".stripMargin,
    """|
       |<val List(x, y) = List(1, 2)> // x: Int = 1, y: Int =...
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
       |<Stream.from(10)> // : Stream[Int] = Stre...
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
       |}> // : Int = 42
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
       |<val n = 10> // : Int = 10
       |n: Int = 10
       |<println(n)> // 10
       |// 10
       |<val m = n * 10> // : Int = 100
       |m: Int = 100
       |""".stripMargin
  )

  checkDecorations(
    "imports",
    """import scala.concurrent.Future
      |val n = Future.successful(10)
      |""".stripMargin,
    """|import scala.concurrent.Future
       |<val n = Future.successful(10)> // : Future[Int] = Futu...
       |n: Future[Int] = Future(Success(10))
       |""".stripMargin
  )

  // From 2.13 we get `name =` part
  val definitionCompat =
    """|case class User(name: String)
       |<val n = User("Susan")> // : User = User(name =...
       |n: User = User(name = "Susan")
       |""".stripMargin

  checkDecorations(
    "definition",
    """case class User(name: String)
      |val n = User("Susan")
      |""".stripMargin,
    """|case class User(name: String)
       |<val n = User("Susan")> // : User = User("Susan...
       |n: User = User("Susan")
       |""".stripMargin,
    compat = Map(
      "0.26" -> definitionCompat,
      "2.13" -> definitionCompat
    )
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
       |""".stripMargin,
    compat = Map(
      "0.26" ->
        """|type-error:2:21: error: Found:    ("not found" : String)
           |Required: Int
           |val filename: Int = "not found"
           |                    ^^^^^^^^^^^
           |""".stripMargin
    )
  )

  checkDiagnostics(
    "crash",
    """
      |def crash(msg: String) = throw new RuntimeException(msg)
      |val filename = "boom"
      |crash(filename)
      |""".stripMargin,
    """|crash:4:1: error: java.lang.RuntimeException: boom
       |	at repl.MdocSession$App.crash(crash.scala:8)
       |	at repl.MdocSession$App.<init>(crash.scala:14)
       |	at repl.MdocSession$.app(crash.scala:3)
       |
       |crash(filename)
       |^^^^^^^^^^^^^^^
       |""".stripMargin,
    compat = Map(
      "0.26" ->
        """|crash:4:1: error: java.lang.RuntimeException: boom
           |	at repl.MdocSession$App.crash(crash.scala:8)
           |	at repl.MdocSession$App.<init>(crash.scala:16)
           |	at repl.MdocSession$.app(crash.scala:3)
           |
           |crash(filename)
           |^^^^^^^^^^^^^^^
           |""".stripMargin
    )
  )

  checkDecorations(
    "partial-exception",
    """
      |val x = "foobar".stripSuffix("bar")
      |throw new RuntimeException("boom")
      |val y = "foobar".stripSuffix("bar")
      |""".stripMargin,
    """|
       |<val x = "foobar".stripSuffix("bar")> // : String = "foo"
       |x: String = "foo"
       |""".stripMargin
  )

  // test doesn't actually work currently
  checkDecorations(
    "fastparse".ignore,
    """
      |import $dep.`com.lihaoyi::fastparse:2.3.0`
      |import fastparse._, MultiLineWhitespace._
      |def p[_:P] = P("a")
      |parse("a", p(_))
      |""".stripMargin,
    """|import $dep.`com.lihaoyi::fastparse:2.3.0`
       |import fastparse._, MultiLineWhitespace._
       |def p[_:P] = P("a")
       |<parse("a", p(_))> // Success((), 1)
       |res0: Parsed[Unit] = Success((), 1)
       |""".stripMargin
  )

  checkDecorations(
    "dotty-extension-methods".tag(OnlyScala3),
    """|case class Circle(x: Double, y: Double, radius: Double)
       |extension (c: Circle)
       |  def circumference: Double = c.radius * math.Pi * 2
       |val circle = Circle(0.0, 0.0, 2.0)
       |circle.circumference
       |extension [T](xs: List[T])
       |  def second = xs.tail.head
       |List(1,2,3).second
       |""".stripMargin,
    """|case class Circle(x: Double, y: Double, radius: Double)
       |extension (c: Circle)
       |  def circumference: Double = c.radius * math.Pi * 2
       |<val circle = Circle(0.0, 0.0, 2.0)> // : Circle = Circle(x ...
       |circle: Circle = Circle(
       |  x = 0.0,
       |  y = 0.0,
       |  radius = 2.0
       |)
       |<circle.circumference> // : Double = 12.566370...
       |res0: Double = 12.566370614359172
       |extension [T](xs: List[T])
       |  def second = xs.tail.head
       |<List(1,2,3).second> // : Int = 2
       |res1: Int = 2
       |""".stripMargin
  )

  checkDecorations(
    "dotty-imports".tag(OnlyScala3),
    """|import $dep.`org.json4s:json4s-native_2.13:3.6.9`
       |import org.json4s._
       |import org.json4s.native.JsonMethods._
       |parse("{ \"numbers\" : [1, 2, 3, 4] }")
       |""".stripMargin,
    """|import $dep.`org.json4s:json4s-native_2.13:3.6.9`
       |import org.json4s._
       |import org.json4s.native.JsonMethods._
       |<parse("{ \"numbers\" : [1, 2, 3, 4] }")> // : JValue = JObject(o...
       |res0: JValue = JObject(
       |  obj = List(
       |    (
       |      "numbers",
       |...
       |""".stripMargin
  )

  def checkDiagnostics(
      options: TestOptions,
      original: String,
      expected: String,
      compat: Map[String, String] = Map.empty
  ): Unit = {
    test(options) {
      val filename = options.name + ".scala"
      val worksheet = mdoc.evaluateWorksheet(filename, original)
      val input = Input.VirtualFile(options.name, original)
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
      assertNoDiff(obtained, Compat(expected, compat))
    }
  }

  def checkDecorations(
      options: TestOptions,
      original: String,
      expected: String,
      compat: Map[String, String] = Map.empty
  ): Unit = {
    test(options) {
      val filename = options.name + ".scala"
      val worksheet = mdoc.evaluateWorksheet(filename, original)
      val statements = worksheet.statements().asScala.sortBy(_.position().startLine())
      val input = Input.VirtualFile(options.name, original)
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
      assertNoDiff(obtained, Compat(expected, compat))
    }
  }
}
