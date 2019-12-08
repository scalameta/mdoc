package tests.cli

import mdoc.internal.cli.MdocProperties
import scala.meta.internal.io.PathIO
import scala.meta.testkit.DiffAssertions
import org.scalatest.funsuite.AnyFunSuite

class MdocPropertiesSuite extends AnyFunSuite with DiffAssertions {
  test("default") {
    val obtained = MdocProperties.default(PathIO.workingDirectory).scalacOptions
    assert(obtained.contains("-deprecation"))
  }

}
