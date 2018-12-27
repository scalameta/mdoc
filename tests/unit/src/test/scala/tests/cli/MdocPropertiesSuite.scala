package tests.cli

import mdoc.internal.cli.MdocProperties
import org.scalatest.FunSuite
import scala.meta.internal.io.PathIO
import scala.meta.testkit.DiffAssertions

class MdocPropertiesSuite extends FunSuite with DiffAssertions {
  test("default") {
    val obtained = MdocProperties.default(PathIO.workingDirectory).scalacOptions
    assert(obtained.contains("-deprecation"))
  }

}
