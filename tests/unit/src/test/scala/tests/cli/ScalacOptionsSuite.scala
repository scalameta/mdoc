package tests.cli

class ScalacOptionsSuite extends BaseCliSuite {
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
      |baz[Either[Int, ?]]
      |```
      |""".stripMargin,
    """|/index.md
       |```scala
       |def baz[T[_]] = ()
       |baz[Either[Int, ?]]
       |```
    """.stripMargin
  )

  // NOTE(olafur): with -Xfatal-warning, the following program reports the following
  // warning if its wrapped in classes.
  // > warning: The outer reference in this type test cannot be checked at run time.
  // We wrap the code in objects instead of classes to avoid this warning.
  val finalInput: String =
    """
      |/in.md
      |```scala mdoc
      |sealed abstract class Maybe[+A] extends Product with Serializable
      |
      |final case class Just[A](value: A) extends Maybe[A]
      |final case object Nothing extends Maybe[Nothing]
      |```
    """.stripMargin
  checkCli(
    "final",
    finalInput,
    extraArgs = Array(
      "--scalac-options",
      "-Ywarn-unused -Xfatal-warnings"
    ),
    expected = finalInput.replaceFirst("scala mdoc", "scala")
  )

  checkCli(
    "-Ywarn-value-discard",
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
       |
       |```scala
       |println("2")
       |// 2
       |```
    """.stripMargin,
    extraArgs = Array(
      "--scalac-options",
      "-Ywarn-value-discard"
    ),
    onStdout = { out =>
      assert(!out.contains("discarded non-Unit value"))
    }
  )
}
