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
import org.scalactic.source.Position

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

  def assertOneSameOrPrintExpected(obtained: String, expected: List[String], title: String = "")(
    implicit source: Position): Boolean = {
    try assertAllDiff(obtained, expected, title)
    catch {
      case ex: Exception =>
        obtained.linesIterator.toList match {
          case head +: tail =>
            println("    \"\"\"|" + head)
            tail.foreach(line => println("       |" + line))
          case head +: Nil =>
            println(head)
          case Nil =>
            println("obtained is empty")
        }
        throw ex
    }
  }

  // scala.meta.testkit.DiffAssertions
  // Cannot access these because they are private
  import collection.JavaConverters._
  import org.scalatest.exceptions.{StackDepthException, TestFailedException}


  def splitIntoLines(string: String): Seq[String] =
    string.trim.replace("\r\n", "\n").split("\n")

  def compareContents(original: String, revised: String): String =
    compareContents(splitIntoLines(original), splitIntoLines(revised))

  def compareContents(original: Seq[String], revised: Seq[String]): String = {
    val diff = difflib.DiffUtils.diff(original.asJava, revised.asJava)
    if (diff.getDeltas.isEmpty) ""
    else
      difflib.DiffUtils
        .generateUnifiedDiff(
          "obtained",
          "expected",
          original.asJava,
          diff,
          1
        )
        .asScala
        .mkString("\n")
  }

  case class DiffFailure(
                                  title: String,
                                  expected: String,
                                  obtained: String,
                                  diff: String,
                                  source: Position
                                ) extends TestFailedException(
    { _: StackDepthException =>
      Some(title + "\n" + error2message(obtained, expected))
    },
    None,
    source
  )

  def header[T](t: T): String = {
    val line = s"=" * (t.toString.length + 3)
    s"$line\n=> $t\n$line"
  }

  def stripTrailingWhitespace(str: String): String = str.replaceAll(" \n", "∙\n")

  private def error2message(obtained: String, expected: String): String = {
    val sb = new StringBuilder
    if (obtained.length < 1000) {
      sb.append(
        s"""#${header("Obtained")}
           #${stripTrailingWhitespace(obtained)}
           #
           #""".stripMargin('#')
      )
    }
    sb.append(
      s"""#${header("Diff")}
         #${stripTrailingWhitespace(compareContents(obtained, expected))}""".stripMargin('#')
    )
    sb.toString()
  }

  def assertAllDiff(obtained: String, expected: List[String], title: String = "")(
    implicit source: Position): Boolean = {
    val theSame = expected.filter( p => compareContents(obtained, p).isEmpty)
    val result = expected.foldLeft("")( (acc,p) => acc + compareContents(obtained, p) )
    if (theSame.nonEmpty) true
    else {
      throw DiffFailure(title, expected.mkString(" OR \n"), obtained, result, source)
    }
  }

  def checkCliMulti(
                name: String,
                original: String,
                expected: List[String],
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
      assertOneSameOrPrintExpected(obtained, expected)
      onStdout(stdout)
    }
  }
}
