package vork

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import scala.meta.internal.io.PathIO
import scala.meta.testkit.DiffAssertions
import scala.meta.io.AbsolutePath
import org.scalatest.FunSuite

case class CliFixture(in: Path, out: Path)
abstract class BaseCliSuite extends FunSuite with DiffAssertions {
  private val cwd = PathIO.workingDirectory
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
      val in = StringFS.string2dir(original)
      val out = Files.createTempDirectory("vork")
      setup(CliFixture(in.toNIO, out))
      val args = Array[String](
        "--in",
        in.toString,
        "--out",
        out.toString,
        "--cwd",
        in.toString,
        "--site.version",
        "\"1.0\""
      )
      val code = Cli.process(args ++ extraArgs, myStdout, in.toNIO)
      val stdout = fansi.Str(myStdout.toString()).plainText
      assert(code == expectedExitCode, stdout)
      val obtained = StringFS.dir2string(AbsolutePath(out))
      assertNoDiff(obtained, expected)
      onStdout(stdout)
    }
  }
}
