package mdoc.internal.livereload

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import mdoc.internal.markdown.Markdown

object SimpleHtml {

  def fromMarkdown(markdown: String, filename: String, url: String): String = {
    val settings = Markdown.plainSettings()
    val parser = Parser.builder(settings).build()
    val renderer = HtmlRenderer.builder(settings).build()
    val body = renderer.render(parser.parse(markdown))
    wrapHtmlBody(body, filename, url)
  }

  private def wrapHtmlBody(body: String, title: String, url: String): String = {
    s"""|<html>
        |<head>
        |    <title>$title</title>
        |    <meta charset="UTF-8">
        |    <link rel="stylesheet" href="$url/github.css">
        |    <script src="$url/highlight.js"></script>
        |    <script>
        |      hljs.configure({languages: []});
        |      hljs.initHighlightingOnLoad();
        |    </script>
        |    <script src="$url/livereload.js"></script>
        |    $baseStyle
        |</head>
        |<body data-spy="scroll" data-target="#toc">
        |<div class="container-fluid">
        |    <div class="row">
        |      <div class="col-sm-3 col-md-2 col-lg-2 col-xl-1">
        |        <nav id="toc" data-toggle="toc" class="sticky-top"></nav>
        |      </div>
        |      <div class="col-sm-9 col-md-10 col-lg-7 col-xl-5">
        |        $body
        |      </div>
        |    </div>
        |  </div>
        |</body>
        |</html>
        |""".stripMargin
  }

  private def baseStyle: String =
    """
      |<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" crossorigin="anonymous">
      |<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js" integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" crossorigin="anonymous"></script>
      |<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js" integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49" crossorigin="anonymous"></script>
      |<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js" integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy" crossorigin="anonymous"></script>
      |<link rel="stylesheet" href="https://cdn.rawgit.com/afeld/bootstrap-toc/v1.0.0/dist/bootstrap-toc.min.css">
      |<script src="https://cdn.rawgit.com/afeld/bootstrap-toc/v1.0.0/dist/bootstrap-toc.min.js"></script>
      |<style>
      |body {
      |  font-family: "Bookman Old Style", /* Windows, MacOS */
      |  "Serifa BT", /* Windows XP. Not the same font, but the overall look is close enough. */
      |  "URW Bookman L", /* Unix+X+FontConfig */
      |  "itc bookman", /* Unix+X */
      |  /* Fallback options */
      |  times, /* Unix+X, MacOS */
      |  serif;
      |}
      |nav[data-toggle='toc'] {
      |  top: 42px;
      |}
      |</style>
    """.stripMargin

}
