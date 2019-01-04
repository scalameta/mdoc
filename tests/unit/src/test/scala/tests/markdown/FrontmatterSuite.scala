package tests.markdown

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

  val deep =
    check(
      "deep",
      """
        |---
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
    """.stripMargin,
      """|---
         |
         |title: Use isNaN when checking for NaN
         |layout: article
         |linters:
         |- name: linter
         |  rules:
         |  - name: UseIsNanNotNanComparison
         |    url:  https://github.com/HairyFotr/linter/blob/master/src/test/scala/LinterPluginTest.scala#L1930
         |- name: scapegoat
         |  rules:
         |  - name: NanComparison
         |
         |---
    """.stripMargin
    )

}
