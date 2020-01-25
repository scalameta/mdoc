package tests.cli

import mdoc.internal.cli.MdocProperties
import munit.FunSuite
import scala.meta.internal.io.PathIO

class MdocPropertiesSuite extends FunSuite {
  test("default") {
    val obtained = MdocProperties.default(PathIO.workingDirectory).scalacOptions
    assert(obtained.contains("-deprecation"))
  }

}
