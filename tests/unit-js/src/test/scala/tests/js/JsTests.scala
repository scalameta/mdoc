package tests.js

object JsTests {
  def suffix(name: String): String =
    s"""|<script type="text/javascript" src="$name.md.js" defer></script>
        |<script type="text/javascript" src="mdoc.js" defer></script>
        |""".stripMargin
}
