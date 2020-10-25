addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.18.0")
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.4.4")

libraryDependencies ++= List(
  "org.jsoup" % "jsoup" % "1.12.1",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
unmanagedSourceDirectories.in(Compile) +=
  baseDirectory.in(ThisBuild).value.getParentFile /
    "mdoc-sbt" / "src" / "main" / "scala"
