package tests.markdown

class FootnoteSuite extends BaseMarkdownSuite {
  check(
    "basic",
    """
      |Paragraph with a footnote reference[^1]
      |
      |[^1]: Footnote text added at the bottom of the document
      |
    """.stripMargin,
    """|Paragraph with a footnote reference[^1]
       |
       |[^1]: Footnote text added at the bottom of the document
    """.stripMargin
  )
}
