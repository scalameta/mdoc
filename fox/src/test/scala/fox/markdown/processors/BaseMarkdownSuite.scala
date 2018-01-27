package fox.markdown.processors

import java.nio.file.Paths

import com.vladsch.flexmark.util.options.MutableDataSet
import fox.{Markdown, Options, Processor}

import scala.meta.testkit.DiffAssertions
import scala.reflect.ClassTag

class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val configPath = Paths.get(getClass.getClassLoader.getResource("fox.conf").toURI)
  private val options = Options.fromDefault(new Options(configPath = configPath)).get
  def getSettings(testName: String): MutableDataSet = {
    val settings = Markdown.default(options)
    settings.set(Processor.PathKey, Some(Paths.get(s"dummy-test-${testName}")))
    settings
  }

  def checkError[T <: Throwable: ClassTag](
      name: String,
      original: String,
      expected: String
  ): Unit = {
    test(name) {
      val intercepted = intercept[T] {
        Markdown.toMarkdown(original, getSettings(name))
      }

      assert(intercepted.getMessage.contains(expected))
    }

  }

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val obtained = Markdown.toMarkdown(original, getSettings(name))
      assertNoDiff(obtained, expected)
    }
  }
}
