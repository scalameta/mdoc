package fox.markdown.processors

import java.nio.file.Paths
import fox.{Markdown, Options}
import scala.meta.testkit.DiffAssertions

class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val configPath = Paths.get(getClass.getClassLoader.getResource("fox.conf").toURI)
  private val options = Options.fromDefault(new Options(configPath = configPath)).get
  def check(original: String, expected: String): Unit = {
    test(original) {
      val obtained = Markdown.toMarkdown(original, options)
      assertNoDiff(obtained, expected)
    }
  }
}
