package vork

import java.nio.file.Files
import java.nio.file.Path
import scala.meta.testkit.DiffAssertions
import org.langmeta.io.AbsolutePath
import org.scalatest.FunSuite

case class CliFixture(in: Path, out: Path)
abstract class BaseCliSuite extends FunSuite with DiffAssertions {
  def checkCli(
      name: String,
      original: String,
      expected: String,
      extraArgs: Array[String] = Array.empty,
      setup: CliFixture => Unit = _ => ()
  ): Unit = {
    test(name) {
      val in = StringFS.string2dir(original)
      val out = Files.createTempDirectory("vork")
      setup(CliFixture(in.toNIO, out))
      val args = Array[String](
        "--in",
        in.toString,
        "--out",
        out.toString,
        "--cwd",
        in.toString
      )
      Cli.main(args ++ extraArgs)
      val obtained = StringFS.dir2string(AbsolutePath(out))
      assertNoDiff(obtained, expected)
    }
  }
}
