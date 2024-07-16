package tests.cli

import java.nio.file.Files
import mdoc.internal.cli.Feedback
import munit.FunSuite
import scala.meta.internal.io.PathIO
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import scala.meta.io.AbsolutePath
import cats.instances.set
import mdoc.internal.cli.Context

class CliArgsSuite extends FunSuite {
  private val reporter = ConsoleReporter.default  

  def checkOK(name: String, args: List[String], onSuccess: Settings => Unit = _ => ())(implicit
      loc: munit.Location
  ): Unit = {
    test(name) {
      val obtained = Settings.fromCliArgs(args, PathIO.workingDirectory.toNIO).get
      onSuccess(obtained)
    }
  }

  def checkError(name: String, args: List[String], expected: String)(implicit
      loc: munit.Location
  ): Unit = {
    test(name) {
      Settings
        .fromCliArgs(args, PathIO.workingDirectory.toNIO)
        .andThen(s => Context.fromSettings(s, reporter))
        .toEither match {
        case Left(obtained) =>
          assertNoDiff(obtained.toString(), expected)
        case Right(ok) =>
          fail(s"Expected error but the arguments parsed successfully.")
      }
    }
  }

  private val tmp = Files.createTempDirectory("mdoc")
  Files.delete(tmp)
  checkError(
    "non-exists",
    List("--in", tmp.toString),
    s"File $tmp does not exist."
  )

  private val in2 = Files.createTempDirectory("mdoc")
  private val out2 = in2.resolve("out")
  checkError(
    "out-subdirectory",
    List("--in", in2.toString, "--out", out2.toString),
    Feedback.outSubdirectoryOfIn(in2, out2)
  )

  private val tmpDirectory = Files.createTempDirectory("mdoc")
  private val tmpFile = Files.createFile(Files.createTempDirectory("mdoc").resolve("readme.md"))
  private val tmpFile2 = Files.createFile(Files.createTempDirectory("mdoc").resolve("readme.md"))
  checkError(
    "in-dir-out-file",
    List("--in", tmpDirectory.toString, "--out", tmpFile.toString),
    Feedback.outputCannotBeRegularFile(AbsolutePath(tmpDirectory), AbsolutePath(tmpFile))
  )
  checkError(
    "in-equal-out",
    List("--in", tmpFile.toString, "--out", tmpFile.toString),
    Feedback.inputEqualOutput(AbsolutePath(tmpFile))
  )

  private val tmpReadmeDirectory = {
    val dir = Files.createTempDirectory("mdoc")
    Files.createDirectories(dir.resolve("readme.md"))
    dir
  }
  checkError(
    "in-file-out-dir",
    List("--in", tmpFile.toString, "--out", tmpReadmeDirectory.toString),
    Feedback.outputCannotBeDirectory(
      AbsolutePath(tmpFile),
      AbsolutePath(tmpReadmeDirectory).resolve("readme.md")
    )
  )
  checkError(
    "in-different-length-out",
    List("--in", tmpFile.toString, "--in", tmpFile2.toString, "--out", tmpDirectory.toString),
    Feedback.inputDifferentLengthOutput(
      List(AbsolutePath(tmpFile), AbsolutePath(tmpFile2)),
      List(AbsolutePath(tmpDirectory))
    )
  )

  checkOK(
    "site-variable",
    List("--site.VERSION", "1.0"),
    onSuccess = { obtained => assertEquals(obtained.site.get("VERSION"), Some("1.0")) }
  )

  checkOK(
    "single-in-no-out",
    List("--in", tmpFile.toString)
  )

  checkOK(
    "single-in-single-out",
    List("--in", tmpFile.toString, tmpDirectory.toString)
  )

  checkOK(
    "--property-file-name",
    List("--property-file-name", "es.properties", tmpFile.toString, tmpDirectory.toString), 
    onSuccess = conf => assertEquals(conf.propertyFileName, "es.properties")
  )

  checkOK(
    "--property-file-name default",
    List(tmpFile.toString, tmpDirectory.toString), 
    onSuccess = conf => assertEquals(conf.propertyFileName, "mdoc.properties")
  )

  checkOK(
    "relative-by-cwd",
    List("--cwd", tmpDirectory.toString(), "--in", "readme.template.md", "--out", "readme.md"),
    onSuccess = { settings =>
      assertEquals(settings.in, List(AbsolutePath(tmpDirectory.resolve("readme.template.md"))))
      assertEquals(settings.out, List(AbsolutePath(tmpDirectory.resolve("readme.md"))))
    }
  )

}
