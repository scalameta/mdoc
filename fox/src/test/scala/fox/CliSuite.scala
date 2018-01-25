package fox

import java.nio.file.Files
import scala.meta.testkit.DiffAssertions
import org.scalatest.FunSuite

class CliSuite extends FunSuite with DiffAssertions {
  test("hello world") {
    val in = Files.createTempDirectory("fox")
    val index = Files.createFile(in.resolve("index.md"))
    Files.write(index, "# Hello ![version]".getBytes())
    val out = Files.createTempDirectory("fox")
    val conf = Files.createFile(in.resolve("fox.conf"))
    Files.write(conf, """site.version = "1.0" """.getBytes())
    val args = Array[String](
      "--in",
      in.toString,
      "--out",
      out.toString,
      "--clean-target",
      "--cwd",
      in.toString
    )
    Cli.main(args)
    val expected = "# Hello 1.0"
    val obtained = new String(Files.readAllBytes(out.resolve(index.getFileName)))
    assertNoDiff(obtained, expected)
  }

}
