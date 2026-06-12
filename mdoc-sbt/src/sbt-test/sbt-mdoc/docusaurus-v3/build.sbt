ThisBuild / scalaVersion := "2.13.18"

enablePlugins(MdocPlugin, DocusaurusPlugin)

moduleName := "mdoc-docusaurus-v3-docs"
mdocOut := (ThisBuild / baseDirectory).value / "website" / "docs"
mdocIn := (ThisBuild / baseDirectory).value / "docs"

TaskKey[Unit]("checkV3CreateSiteResults") := {
  val base = (ThisBuild / baseDirectory).value
  val generatedDocs = IO.read(base / "website" / "docs" / "readme.md")
  val expectedDocs =
    """|---
       |slug: /
       |---
       |
       |# Docusaurus v3
       |
       |```scala
       |val answer = 40 + 2
       |// answer: Int = 42
       |```
       |""".stripMargin
  assert(generatedDocs == expectedDocs, generatedDocs)
}
