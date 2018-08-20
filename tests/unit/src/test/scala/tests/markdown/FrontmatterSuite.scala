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

}
