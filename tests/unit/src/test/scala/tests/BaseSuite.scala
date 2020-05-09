package tests

import munit.FunSuite

class BaseSuite extends FunSuite {
  object OnlyScala213 extends munit.Tag("OnlyScala213")
  override def munitTestTransforms: List[TestTransform] = super.munitTestTransforms ++ List(
    new TestTransform(OnlyScala213.value, { test =>
      if (test.tags(OnlyScala213) && mdoc.internal.BuildInfo.scalaBinaryVersion != "2.13")
        test.tag(munit.Ignore)
      else test
    })
  )
}
