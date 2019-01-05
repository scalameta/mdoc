package tests.markdown

import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.formatter.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import mdoc.internal.markdown.Markdown
import scala.collection.JavaConverters._

class FrontmatterSuite extends BaseMarkdownSuite {

  check(
    "yaml",
    """
      |---
      |id: intro
      |title: Foo
      |---
      |
      |YAML
    """.stripMargin.trim,
    """
      |---
      |id: intro
      |title: Foo
      |---
      |
      |YAML
      |""".stripMargin
  )

  val nested =
    """|---
       |title: Use isNaN when checking for NaN
       |layout: article
       |linters:
       |  - name: linter
       |    rules:
       |      - name: UseIsNanNotNanComparison
       |        url:  https://github.com/HairyFotr/linter/blob/master/src/test/scala/LinterPluginTest.scala#L1930
       |  - name: scapegoat
       |    rules:
       |      - name: NanComparison
       |---
       |
       |# Title
    """.stripMargin
  // See https://github.com/vsch/flexmark-java/issues/293
  check("nested", nested, nested)

}
