package fox.markdown.processors

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import com.vladsch.flexmark.util.options.MutableDataSet
import fox.{Markdown, Options, Processor}
import scala.meta.testkit.DiffAssertions
import scala.reflect.ClassTag

class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val configPath = Paths.get(getClass.getClassLoader.getResource("fox.conf").toURI)
  private val options = Options.fromDefault(new Options(configPath = configPath)).get
  def getSettings(path: Path): MutableDataSet = {
    val settings = Markdown.default(options)
    settings.set(Processor.PathKey, Some(path))
    settings
  }

  def getPath(testName: String, original: String): Path = {
    val file = Files.createTempFile("fox", testName)
    Files.write(file, original.getBytes())
    file
  }

  def checkError[T <: Throwable: ClassTag](
      name: String,
      original: String,
      expected: String
  ): Unit = {
    test(name) {
      val path = getPath(name, original)
      val intercepted = intercept[T] {
        Markdown.toMarkdown(original, getSettings(path))
      }
      val msg = intercepted.getMessage.replace(path.toString, "<path>")
      assertNoDiff(msg, expected)
    }
  }

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val path = getPath(name, original)
      val obtained = Markdown.toMarkdown(original, getSettings(path))
      assertNoDiff(obtained, expected)
    }
  }
}
