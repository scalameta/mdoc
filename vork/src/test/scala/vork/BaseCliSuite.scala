package vork

import java.nio.file.Files
import scala.meta.testkit.DiffAssertions
import org.langmeta.io.AbsolutePath
import org.scalatest.FunSuite

abstract class BaseCliSuite extends FunSuite with DiffAssertions {
  def checkCli(
      name: String,
      original: String,
      expected: String,
      extraArgs: Array[String] = Array.empty
  ): Unit = {
    test(name) {
      val in = StringFS.string2dir(original)
      val out = Files.createTempDirectory("vork")
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
}
