package tests.js

object JsTests {
  // MdocPlugin writes these for the unit-js rows; see mdocPropertiesPrefix in build.sbt
  val propertyFileName = "js-mdoc.properties"
  val esPropertyFileName = "js-es.properties"

  def suffix(name: String): String =
    s"""|<script type="text/javascript" src="$name.md.js" defer></script>
        |<script type="text/javascript" src="mdoc.js" defer></script>
        |""".stripMargin
}
