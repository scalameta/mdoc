package tests.cli

import java.nio.file.Files
import mdoc.internal.cli.Feedback
import scala.meta.internal.io.PathIO
import scala.meta.testkit.DiffAssertions
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import org.scalatest.funsuite.AnyFunSuite

class CliArgsSuite extends AnyFunSuite with DiffAssertions {
  private val reporter = ConsoleReporter.default
  private val base = Settings.default(PathIO.workingDirectory)

  def checkOk(args: List[String], isExpected: Settings => Boolean): Unit = {
    test(args.mkString(" ")) {
      val obtained = Settings.fromCliArgs(args, base).get
      assert(isExpected(obtained))
    }
  }

  def checkError(args: List[String], expected: String): Unit = {
    test(args.mkString(" ")) {
      Settings.fromCliArgs(args, base).andThen(_.validate(reporter)).toEither match {
        case Left(obtained) =>
          assertNoDiff(obtained.toString(), expected)
        case Right(ok) =>
          fail(s"Expected error. Obtained $ok")
      }
    }
  }

  private val tmp = Files.createTempDirectory("mdoc")
  Files.delete(tmp)
  checkError(
    "--in" :: tmp.toString :: Nil,
    s"File $tmp does not exist."
  )

  checkOk(
    "--site.VERSION" :: "1.0" :: Nil,
    _.site == Map("VERSION" -> "1.0")
  )

  private val in2 = Files.createTempDirectory("mdoc")
  private val out2 = in2.resolve("out")
  checkError(
    "--in" :: in2.toString :: "--out" :: out2.toString :: Nil,
    Feedback.outSubdirectoryOfIn(in2, out2)
  )

}
