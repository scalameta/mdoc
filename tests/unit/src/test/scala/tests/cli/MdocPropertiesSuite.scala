package tests.cli

import mdoc.internal.cli.MdocProperties
import org.scalatest.FunSuite
import scala.meta.testkit.DiffAssertions

class MdocPropertiesSuite extends FunSuite with DiffAssertions {
  test("default") {
    val obtained = MdocProperties.default().scalacOptions
    assert(obtained.contains("-deprecation"))
  }

}
