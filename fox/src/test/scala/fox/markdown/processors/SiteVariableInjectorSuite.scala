package fox.markdown.processors

class SiteVariableInjectorSuite extends BaseMarkdownSuite {
  check(
    """
      |# Hey ![version]
    """.stripMargin,
    """
      |# Hey 1.0
    """.stripMargin
  )

  check(
    """
      |I am ![version]
    """.stripMargin,
    """
      |I am 1.0
    """.stripMargin
  )

  check(
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
}
