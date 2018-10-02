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
          |warning: index.md:3:25: warning: Unused import
          |import scala.concurrent.Future
          |                        ^^^^^^""".stripMargin
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

}
