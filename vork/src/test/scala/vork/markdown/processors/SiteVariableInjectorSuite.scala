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

  check(
    "markdown-safe-buffer",
    """# safe-buffer [![travis][travis-image]][travis-url] [![npm][npm-image]][npm-url] [![downloads][downloads-image]][downloads-url] [![javascript style guide][standard-image]][standard-url]
      |
      |[travis-image]: https://img.shields.io/travis/feross/safe-buffer/master.svg
      |[travis-url]: https://travis-ci.org/feross/safe-buffer
      |[npm-image]: https://img.shields.io/npm/v/safe-buffer.svg
      |[npm-url]: https://npmjs.org/package/safe-buffer
      |[downloads-image]: https://img.shields.io/npm/dm/safe-buffer.svg
      |[downloads-url]: https://npmjs.org/package/safe-buffer
      |[standard-image]: https://img.shields.io/badge/code_style-standard-brightgreen.svg
      |[standard-url]: https://standardjs.com
    """.stripMargin,
    """# safe-buffer [![travis][travis-image]][travis-url] [![npm][npm-image]][npm-url] [![downloads][downloads-image]][downloads-url] [![javascript style guide][standard-image]][standard-url]
      |
      |[travis-image]: https://img.shields.io/travis/feross/safe-buffer/master.svg
      |[travis-url]: https://travis-ci.org/feross/safe-buffer
      |[npm-image]: https://img.shields.io/npm/v/safe-buffer.svg
      |[npm-url]: https://npmjs.org/package/safe-buffer
      |[downloads-image]: https://img.shields.io/npm/dm/safe-buffer.svg
      |[downloads-url]: https://npmjs.org/package/safe-buffer
      |[standard-image]: https://img.shields.io/badge/code_style-standard-brightgreen.svg
      |[standard-url]: https://standardjs.com
    """.stripMargin
  )
}
