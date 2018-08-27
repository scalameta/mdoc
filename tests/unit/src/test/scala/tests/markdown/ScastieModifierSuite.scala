package tests.markdown

import mdoc.modifier.ScastieModifier
import mdoc.StringModifier
import mdoc.internal.cli.Settings

class ScastieModifierSuite extends BaseMarkdownSuite {

  private val debugClassSuffix = "test"

  def settingsWithModifier(s: StringModifier): Settings =
    settings.copy(stringModifiers = settings.stringModifiers :+ s)

  val defaultSettings: Settings = settingsWithModifier(new ScastieModifier(
    debugClassSuffix = Some(debugClassSuffix)
  ))

  val darkThemeSettings: Settings = settingsWithModifier(new ScastieModifier(
    theme = "dark",
    debugClassSuffix = Some(debugClassSuffix)
  ))

  checkWithSettings(defaultSettings)(
    "inline",
    """
      |```scala mdoc:scastie
      |val x = 1
      |```
    """.stripMargin,
    s"""
       |<script src="https://scastie.scala-lang.org/embedded.js"></script>
       |
       |<pre class='scastie-snippet-${debugClassSuffix}'></pre>
       |
       |<script>window.addEventListener('load', function() {
       |scastie.Embedded('.scastie-snippet-${debugClassSuffix}', {
       |code: `val x = 1`,
       |theme: 'light',
       |isWorksheetMode: true,
       |targetType: 'jvm',
       |scalaVersion: '2.12.6'
       |})
       |})</script>
    """.stripMargin
  )

  checkWithSettings(darkThemeSettings)(
    "inline with dark theme",
    """
      |```scala mdoc:scastie
      |val x = 1
      |```
    """.stripMargin,
    s"""
       |<script src="https://scastie.scala-lang.org/embedded.js"></script>
       |
       |<pre class='scastie-snippet-${debugClassSuffix}'></pre>
       |
       |<script>window.addEventListener('load', function() {
       |scastie.Embedded('.scastie-snippet-${debugClassSuffix}', {
       |code: `val x = 1`,
       |theme: 'dark',
       |isWorksheetMode: true,
       |targetType: 'jvm',
       |scalaVersion: '2.12.6'
       |})
       |})</script>
    """.stripMargin
  )

  checkWithSettings(defaultSettings)(
    "snippet",
    """
      |```scala mdoc:scastie:xbrvky6fTjysG32zK6kzRQ
      |
      |```
    """.stripMargin,
    s"""
       |<script src='https://scastie.scala-lang.org/xbrvky6fTjysG32zK6kzRQ.js?theme=light'></script>
    """.stripMargin
  )

  checkWithSettings(darkThemeSettings)(
    "snippet with dark theme",
    """
      |```scala mdoc:scastie:xbrvky6fTjysG32zK6kzRQ
      |
      |```
    """.stripMargin,
    s"""
       |<script src='https://scastie.scala-lang.org/xbrvky6fTjysG32zK6kzRQ.js?theme=dark'></script>
    """.stripMargin
  )

}
