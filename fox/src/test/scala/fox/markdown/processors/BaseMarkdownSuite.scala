package fox.markdown.processors

import fox.{Markdown, Options}

import scala.meta.testkit.DiffAssertions

class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val configPath = getClass.getClassLoader.getResource("fox.conf").toURI.getPath()
  private val options = new Options(configPath = configPath)
  def check(original: String, expected: String): Unit = {
    test(original) {
      val obtained = Markdown.toMarkdown(original, options)
      assertNoDiff(obtained, expected)
    }
  }
}
