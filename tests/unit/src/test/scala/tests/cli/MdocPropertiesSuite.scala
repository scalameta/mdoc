package tests.cli

import mdoc.internal.cli.MdocProperties
import munit.FunSuite
import scala.meta.internal.io.PathIO
import mdoc.internal.BuildInfo

class MdocPropertiesSuite extends FunSuite {
  test("default") {
    val obtained = MdocProperties.default(PathIO.workingDirectory).scalacOptions

    if (BuildInfo.scalaVersion.startsWith("3.0"))
      assert(obtained.contains("-language:implicitConversions"))
    else
      assert(obtained.contains("-deprecation"))
  }

}
