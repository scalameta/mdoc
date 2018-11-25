package mdoc.internal.lsp

import mdoc.internal.livereload.TableOfContents

object PreviewHtml {
  def wrapHtmlBody(
      body: String,
      tableOfContents: TableOfContents,
      title: String,
      url: String
  ): String = {
    val toc = tableOfContents.toHTML(fromLevel = 2, toLevel = 3, indent = "      ")
    val sidebar =
      if (toc.trim.isEmpty) ""
      else {
        s"""
           |    <div class="sidebar">
           |      <h2>Table of contents</h2>
           |      $toc
           |    </div>
      """.stripMargin
      }
    s"""|<!DOCTYPE html>
        |<html>
        |<head>
        |    <title>$title</title>
        |    <meta charset="UTF-8">
        |    <link rel="stylesheet" href="$url/github.css">
        |    <link rel="stylesheet" href="$url/preview.css">
        |    <script src="$url/highlight.js"></script>
        |    <script>
        |      hljs.configure({languages: []});
        |      hljs.initHighlightingOnLoad();
        |    </script>
        |</head>
        |<body>
        |  <div class="wrapper">
        |    <div class="main">
        |      $body
        |    </div>
        |    <hr>
        |  </div>
        |  $sidebar
        |</body>
        |</html>
        |""".stripMargin
  }
}
