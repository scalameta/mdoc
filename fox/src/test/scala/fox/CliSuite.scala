package fox

import java.nio.file.Files
import scala.meta.testkit.DiffAssertions
import fox.internal.BuildInfo
import org.langmeta.io.AbsolutePath
import org.scalatest.FunSuite

class CliSuite extends FunSuite with DiffAssertions {
  def checkCli(
      name: String,
      original: String,
      expected: String,
      extraArgs: Array[String] = Array.empty,
      setup: () => Unit = () => ()
  ): Unit = {
    test(name) {
      val in = StringFS.string2dir(original)
      val out = Files.createTempDirectory("fox")
      val args = Array[String](
        "--in",
        in.toString,
        "--out",
        out.toString,
        "--clean-target",
        "--cwd",
        in.toString
      )
      Cli.main(args ++ extraArgs)
      val obtained = StringFS.dir2string(AbsolutePath(out))
      assertNoDiff(obtained, expected)
    }
  }

  checkCli(
    "fox.conf",
    """
      |/index.md
      |# Hello ![version]
      |/fox.conf
      |site.version = "1.0"
    """.stripMargin,
    """
      |/index.md
      |# Hello 1.0
    """.stripMargin
  )

  checkCli(
    "classpath",
    """
      |/index.md
      |```scala fox
      |test.Test.greeting
      |```
    """.stripMargin,
    """
      |/index.md
      |```scala
      |@ test.Test.greeting
      |res0: String = "Hello world!"
      |```
    """.stripMargin,
    extraArgs = Array(
      "--classpath",
      BuildInfo.testsInputClassDirectory.getAbsolutePath
    )
  )

}
