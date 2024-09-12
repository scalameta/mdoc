addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.12.0")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.6.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.16.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.5")

addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")

libraryDependencies ++= List(
  "org.jsoup" % "jsoup" % "1.12.1",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "mdoc-sbt" / "src" / "main" / "scala"
