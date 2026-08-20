addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")

libraryDependencies ++= List(
  "org.jsoup" % "jsoup" % "1.23.1",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
Compile / unmanagedSourceDirectories ++= {
  val dir = (ThisBuild / baseDirectory).value.getParentFile / "mdoc-sbt" / "src" / "main"
  // the meta-build is Scala 2 on sbt 1 and Scala 3 on sbt 2, and the plugin
  // keeps a compat shim for each
  val variant = if (scalaBinaryVersion.value == "3") "scala-3" else "scala-2"
  Seq(dir / "scala", dir / variant)
}
