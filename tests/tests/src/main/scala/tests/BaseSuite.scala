package tests

import munit.FunSuite
import munit.Location
import tests.markdown.Compat

class BaseSuite extends FunSuite {
  def postProcessObtained: Map[Compat.ScalaVersion, String => String] = Map.empty
  def postProcessExpected: Map[Compat.ScalaVersion, String => String] = Map.empty
  override def assertNoDiff(obtained: String, expected: String, clue: => Any)(implicit
      loc: Location
  ): Unit = {
    super.assertNoDiff(
      Compat(processOutput(obtained), Map.empty, postProcessObtained),
      Compat(processOutput(expected), Map.empty, postProcessExpected),
      clue
    )
  }

  private def processOutput(raw: String) =
    raw.replaceAll("\t", " ")

  object OnlyScala213 extends munit.Tag("OnlyScala213")
  object OnlyScala3 extends munit.Tag("OnlyScala3")
  object SkipScala3 extends munit.Tag("SkipScala3")
  object SkipScala211 extends munit.Tag("SkipScala211")
  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms ++ List(
      new TestTransform(
        "ScalaVersions",
        { test =>
          val binaryVersion = tests.BuildInfo.scalaBinaryVersion
          if (test.tags(OnlyScala213) && binaryVersion != "2.13")
            test.tag(munit.Ignore)
          else if (test.tags(OnlyScala3) && binaryVersion != "3")
            test.tag(munit.Ignore)
          else if (test.tags(SkipScala3) && binaryVersion == "3")
            test.tag(munit.Ignore)
          else if (test.tags(SkipScala211) && binaryVersion == "2.11")
            test.tag(munit.Ignore)
          else test
        }
      )
    )
}
