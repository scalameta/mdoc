package mdoc.internal.livereload

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import mdoc.internal.markdown.Markdown

object SimpleHtml {

  def fromMarkdown(markdown: String, filename: String, url: String): String = {
    val settings = Markdown.plainSettings()
    val parser = Parser.builder(settings).build()
    val renderer = HtmlRenderer.builder(settings).build()
    val document = parser.parse(markdown)
    val body = renderer.render(document)
    val toc = TableOfContents(document)
    wrapHtmlBody(body, toc, filename, url)
  }

  def wrapHtmlBody(
      body: String,
      tableOfContents: TableOfContents,
      title: String,
      url: String
  ): String = {
    val toc = tableOfContents.toHTML(fromLevel = 2, toLevel = 3, indent = "      ")
    s"""
<html>
<head>
    <title>$title</title>
    <meta charset="UTF-8">
    <link rel="stylesheet" href="$url/github.css">
    <link rel="stylesheet" href="$url/custom.css">
    <script src="$url/highlight.js"></script>
    <script>
      hljs.configure({languages: []});
      hljs.initHighlightingOnLoad();
    </script>
    <script src="$url/livereload.js"></script>
</head>
<body>
  <div class="wrapper">
    <div class="main">
      $body
    </div>
    <div class="sidebar">
      $toc
    </div>
  </div>
</body>
</html>
"""
  }

}
