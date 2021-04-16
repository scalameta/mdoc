package tests.cli

import tests.markdown.Compat

class ScalacOptionsSuite extends BaseCliSuite {
  if (Compat.isScala212)
    checkCli(
      "-Ywarn-unused-import",
      """
        |/index.md
        |# Hello
        |```scala mdoc
        |import scala.concurrent.Future
        |```
    """.stripMargin,
      "",
      extraArgs = Array(
        "--report-relative-paths",
        "--scalac-options",
        "-Ywarn-unused -Xfatal-warnings"
      ),
      expectedExitCode = 1,
      onStdout = { out =>
        val expected =
          """
            |warning: index.md:3:1: Unused import
            |import scala.concurrent.Future
            |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^""".stripMargin
        assert(out.contains(expected))
      }
    )

  checkCli(
    "kind-projector",
    """
      |/index.md
      |```scala mdoc
      |def baz[T[_]] = ()
      |baz[Either[Int, *]]
      |```
      |""".stripMargin,
    """|/index.md
       |```scala
       |def baz[T[_]] = ()
       |baz[Either[Int, *]]
       |```
    """.stripMargin,
    extraArgs =
      if (Compat.isScala3)
        Array(
          "--scalac-options",
          "-Ykind-projector"
        )
      else Array()
  )

  // NOTE(olafur): with -Xfatal-warning, the following program reports the following
  // warning if its wrapped in classes.
  // > warning: The outer reference in this type test cannot be checked at run time.
  // We wrap the code in objects instead of classes to avoid this warning.
  //
  // Note(anton): this used to say "final case object", but in Scala3 this produces a warning
  // that final objects are redundant
  val finalInput: String =
    """
      |/in.md
      |```scala mdoc:reset-object
      |sealed abstract class Maybe[+A] extends Product with Serializable
      |
      |final case class Just[A](value: A) extends Maybe[A]
      |case object Nothing extends Maybe[Nothing]
      |```
    """.stripMargin
  checkCli(
    "final",
    finalInput,
    extraArgs = Array(
      "--scalac-options",
      "-Ywarn-unused -Xfatal-warnings"
    ),
    expected = finalInput.replaceFirst("scala mdoc:reset-object", "scala")
  )

  // Removed in Scala3
  checkCli(
    "-Ywarn-value-discard".tag(SkipScala3),
    """
      |/index.md
      |```scala mdoc
      |println("1")
      |```
      |```scala mdoc:reset
      |println("2")
      |```
      |""".stripMargin,
    """|/index.md
       |```scala
       |println("1")
       |// 1
       |```
       |```scala
       |println("2")
       |// 2
       |```
    """.stripMargin,
    extraArgs = Array(
      "--scalac-options",
      "-Ywarn-value-discard"
    ),
    onStdout = { out => assert(!out.contains("discarded non-Unit value")) }
  )

  // TODO: fix for Scala 3
  checkCli(
    "no-imports".tag(SkipScala3),
    """
      |/index.md
      |```scala mdoc
      |scala.List(1)
      |```
      |""".stripMargin,
    """|/index.md
       |```scala
       |scala.List(1)
       |// res0: scala.collection.immutable.List[scala.Int] = List(1)
       |```
       |
    """.stripMargin,
    extraArgs = Array(
      "--scalac-options",
      "-Yno-imports"
    )
  )

  // see https://github.com/scalameta/mdoc/issues/151
  checkCli(
    "dead-fail",
    """
      |/index.md
      |```scala mdoc
      |final case class Test(value: Int)
      |
      |val test = Test(123)
      |
      |test.value
      |```
      |
      |```scala mdoc:fail
      |val x: Int = "123"
      |```
      |""".stripMargin,
    """|/index.md
       |```scala
       |final case class Test(value: Int)
       |
       |val test = Test(123)
       |// test: Test = Test(123)
       |
       |test.value
       |// res0: Int = 123
       |```
       |
       |```scala
       |val x: Int = "123"
       |// error: type mismatch;
       |//  found   : String("123")
       |//  required: Int
       |// val x: Int = "123"
       |//              ^^^^^
       |```
       |""".stripMargin,
    extraArgs = Array(
      "--scalac-options",
      "-Ywarn-value-discard"
    ),
    compat = Map(
      "2.13" ->
        """|/index.md
           |```scala
           |final case class Test(value: Int)
           |
           |val test = Test(123)
           |// test: Test = Test(value = 123)
           |
           |test.value
           |// res0: Int = 123
           |```
           |
           |```scala
           |val x: Int = "123"
           |// error: type mismatch;
           |//  found   : String("123")
           |//  required: Int
           |// val x: Int = "123"
           |//              ^^^^^
           |```
           |""".stripMargin,
      "3.0" ->
        """|/index.md
           |```scala
           |final case class Test(value: Int)
           |
           |val test = Test(123)
           |// test: Test = Test(123)
           |
           |test.value
           |// res0: Int = 123
           |```
           |
           |```scala
           |val x: Int = "123"
           |// error:
           |// Found:    ("123" : String)
           |// Required: Int
           |// val x: Int = "123"
           |//              ^^^^^
           |```
           |""".stripMargin
    )
  )

}
