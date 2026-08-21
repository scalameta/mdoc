package tests.js

import java.nio.file._

object JsTests {
  // MdocPlugin writes these for the unit-js rows; see mdocPropertiesPrefix in build.sbt
  val propertyFileName = "js-mdoc.properties"
  val esPropertyFileName = "js-es.properties"

  def suffix(name: String): String =
    s"""|<script type="text/javascript" src="$name.md.js" defer></script>
        |<script type="text/javascript" src="mdoc.js" defer></script>
        |""".stripMargin

  /** A test resource as a file on disk.
    *
    * sbt 2 puts test resources in a jar, where Paths.get on the URL fails, so the bytes are copied
    * out.
    */
  def importMap: Path = {
    val name = "importmap.json"
    val in = getClass.getClassLoader.getResourceAsStream(name)
    require(in != null, s"no such resource: $name")
    try {
      val out = Files.createTempFile("mdoc-", "-" + name)
      Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING)
      out.toAbsolutePath()
    } finally in.close()
  }

}
