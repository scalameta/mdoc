package vork

import java.nio.file.Files
import org.langmeta.internal.io.PathIO
import scala.meta.testkit.DiffAssertions
import org.scalatest.FunSuite

class CliArgsSuite extends FunSuite with DiffAssertions {
  private val logger = Logger.default
  private val base = Args.default(PathIO.workingDirectory)

  def checkError(args: List[String], expected: String): Unit = {
    test(args.mkString(" ")) {
      Args.fromCliArgs(args, logger, base).toEither match {
        case Left(obtained) =>
          assertNoDiff(obtained.toString(), expected)
        case Right(ok) =>
          fail(s"Expected error. Obtained $ok")
      }
    }
  }

  checkError(
    "--include-files" :: "*" :: Nil,
    // TODO(olafur) automatically include flag name in metaconfig error message
    """|Dangling meta character '*' near index 0
       |*
       |^
       |""".stripMargin
  )

  private val tmp = Files.createTempDirectory("vork")
  Files.delete(tmp)
  checkError(
    "--in" :: tmp.toString :: Nil,
    s"File $tmp does not exist."
  )

}
