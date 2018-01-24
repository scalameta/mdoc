package fox.markdown.processors

class VariableSplicerSuite extends BaseMarkdownSuite {
  check(
    """
      |# Hey ![site.version]
    """.stripMargin,
    """
      |# Hey 1.0
    """.stripMargin
  )

  check(
    """
      |I am ![site.version]
    """.stripMargin,
    """
      |I am 1.0
    """.stripMargin
  )

  check(
    """
      || C1 | C2 |
      || == | == |
      || ![site.version] | hello |
    """.stripMargin,
    """
      |
      || C1 | C2 |
      || == | == |
      || 1.0 | hello |
    """.stripMargin
  )
}
