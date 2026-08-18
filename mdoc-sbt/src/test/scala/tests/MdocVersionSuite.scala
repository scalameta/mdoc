package tests

import mdoc.MdocVersion
import munit.FunSuite
import sbt.librarymanagement.CrossVersion

class MdocVersionSuite extends FunSuite {
  val supported = Set("2.12.21", "2.13.18", "3.3.8", "3.8.4")
  val current = "2.9.0"
  val lastBinary = "2.8.2"

  test("supported Scala versions use the current mdoc with CrossVersion.full") {
    val module = MdocVersion.module("mdoc", "2.13.18", current, lastBinary, supported)
    assertEquals(module.revision, current)
    assertEquals(module.crossVersion, CrossVersion.full)
    assertEquals(
      MdocVersion.artifactVersion("3.3.8", current, lastBinary, supported),
      current
    )
  }

  test("unsupported Scala versions fall back to the last binary-published mdoc") {
    val module = MdocVersion.module("mdoc", "2.13.12", current, lastBinary, supported)
    assertEquals(module.revision, lastBinary)
    assertEquals(module.crossVersion, CrossVersion.binary)
    assertEquals(
      MdocVersion.artifactVersion("2.12.12", current, lastBinary, supported),
      lastBinary
    )
  }

  test("empty supported list uses CrossVersion.full so the meta-build does not mix suffixes") {
    val module = MdocVersion.module("mdoc", "2.13.18", current, lastBinary, Set.empty)
    assertEquals(module.revision, current)
    assertEquals(module.crossVersion, CrossVersion.full)
  }
}
