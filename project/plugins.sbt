addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.14.0")
libraryDependencies ++= List(
  "org.jsoup" % "jsoup" % "1.12.1",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
unmanagedSourceDirectories.in(Compile) +=
  baseDirectory.in(ThisBuild).value.getParentFile /
    "mdoc-sbt" / "src" / "main" / "scala"
