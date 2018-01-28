package vork.markdown.processors

class SiteVariableInjectorSuite extends BaseMarkdownSuite {
  check(
    "header",
    """
      |# Hey ![version]
    """.stripMargin,
    """
      |# Hey 1.0
    """.stripMargin
  )

  check(
    "paragraph",
    """
      |I am ![version]
    """.stripMargin,
    """
      |I am 1.0
    """.stripMargin
  )

  check(
    "table",
    """
      || C1 | C2 |
      || == | == |
      || ![version] | hello |
    """.stripMargin,
    """
      |
      || C1 | C2 |
      || == | == |
      || 1.0 | hello |
    """.stripMargin
  )

  // We're missing one leading `!` in the expected, issue has been reported upstream
  check(
    "travis",
    "# Title ![travis][travis-image]",
    "# Title [travis][travis-image]"
  )
}
