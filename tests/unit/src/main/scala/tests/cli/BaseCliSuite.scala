package tests.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import munit.FunSuite
import scala.meta.io.AbsolutePath
import mdoc.Main
import scala.meta.internal.io.PathIO
import scala.meta.io.RelativePath
import munit.TestOptions
import tests.BaseSuite
import tests.markdown.Compat

abstract class BaseCliSuite extends BaseSuite {
  class TemporaryDirectory(name: String) extends Fixture[AbsolutePath](name) {
    var path: AbsolutePath = _
    def apply(): AbsolutePath = path
    override def beforeEach(context: BeforeEach): Unit = {
      path = AbsolutePath(Files.createTempDirectory(name))
    }
  }
  val in = new TemporaryDirectory("in")
  val out = new TemporaryDirectory("out")
  override def postProcessObtained: Map[Compat.ScalaVersion, String => String] =
    Map(
      Compat.All -> { old =>
        {
          val outDir = out().toString().replace("\\", "/")
          val inDir = in().toString().replace("\\", "/")
          old
            .replace("\\", "/")
            .replace(outDir, "<output>")
            .replace(inDir, "<input>")
            .linesIterator
            .filterNot(line => line.startsWith("info: Compiled in"))
            .mkString("\n")
        }
      }
    )
  override def munitFixtures: Seq[Fixture[_]] = List(in, out)
  private val myStdout = new ByteArrayOutputStream()
  def checkCli(
      name: TestOptions,
      original: String,
      expected: String,
      extraArgs: => Array[String] = Array.empty,
      setup: () => Unit = () => (),
      input: => String = in().toString,
      output: => String = out().toString,
      expectedExitCode: => Int = 0,
      onStdout: String => Unit = _ => (),
      includeOutputPath: RelativePath => Boolean = _ => true,
      compat: Map[Compat.ScalaVersion, String] = Map.empty
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      myStdout.reset()
      StringFS.fromString(original, in())
      setup()
      val args = Array[String](
        "--in",
        input,
        "--out",
        output,
        "--cwd",
        in().syntax,
        "--site.version",
        "1.0.0"
      )
      val code = Main.process(args ++ extraArgs, new PrintStream(myStdout), in().toNIO)
      val stdout = fansi.Str(myStdout.toString()).plainText
      assertEquals(code, expectedExitCode, clues(stdout))
      val obtained = StringFS.asString(out(), includePath = includeOutputPath)
      assertNoDiff(obtained, Compat(expected, compat))
      onStdout(stdout)
    }
  }
}
