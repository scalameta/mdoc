package tests.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import org.scalatest.FunSuite
import scala.meta.io.AbsolutePath
import scala.meta.testkit.DiffAssertions
import scala.meta.testkit.StringFS
import mdoc.Main

case class CliFixture(in: Path, out: Path)

abstract class BaseCliSuite extends FunSuite with DiffAssertions {
  private val myStdout = new ByteArrayOutputStream()
  def checkCli(
      name: String,
      original: String,
      expected: String,
      extraArgs: Array[String] = Array.empty,
      setup: CliFixture => Unit = _ => (),
      expectedExitCode: Int = 0,
      onStdout: String => Unit = _ => ()
  ): Unit = {
    test(name) {
      myStdout.reset()
      val in = StringFS.fromString(original)
      val out = Files.createTempDirectory("mdoc")
      setup(CliFixture(in.toNIO, out))
      val args = Array[String](
        "--in",
        in.toString,
        "--out",
        out.toString,
        "--cwd",
        in.toString,
        "--site.version",
        "1.0.0"
      )
      val code = Main.process(args ++ extraArgs, new PrintStream(myStdout), in.toNIO)
      val stdout = fansi.Str(myStdout.toString()).plainText
      assert(code == expectedExitCode, stdout)
      val obtained = StringFS.asString(AbsolutePath(out))
      assertNoDiffOrPrintExpected(obtained, expected)
      onStdout(stdout)
    }
  }
}
