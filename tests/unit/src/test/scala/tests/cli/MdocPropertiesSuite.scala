package tests.cli

import mdoc.internal.cli.MdocProperties
import munit.FunSuite
import scala.meta.internal.io.PathIO
import tests.markdown.Compat

class MdocPropertiesSuite extends FunSuite {
  test("default") {
    val obtained = MdocProperties.default(PathIO.workingDirectory).scalacOptions

    if (Compat.isScala3)
      assert(obtained.contains("-language:implicitConversions"))
    else
      assert(obtained.contains("-deprecation"))
  }

}
