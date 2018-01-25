package fox.markdown.processors

import java.nio.file.Paths
import fox.{Markdown, Options}
import scala.meta.testkit.DiffAssertions
import scala.reflect.ClassTag

class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val configPath = Paths.get(getClass.getClassLoader.getResource("fox.conf").toURI)
  private val options = Options.fromDefault(new Options(configPath = configPath)).get
  def checkError[T <: AnyRef: ClassTag](name: String, original: String): Unit = {
    test(name) {
      intercept[T] {
        Markdown.toMarkdown(original, options)
      }
    }

  }
  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val obtained = Markdown.toMarkdown(original, options)
      assertNoDiff(obtained, expected)
    }
  }
}
