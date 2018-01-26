inThisBuild(
  scalaVersion := "2.12.4"
)

lazy val root = project
  .in(file("."))
  .settings(name := "foxRoot")
  .aggregate(fox)

lazy val fox = project
  .settings(
    fork in run := true,
    cancelable in Global := true,
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.geirsson" %% "metaconfig-core" % "0.6.0",
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.6.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "com.lihaoyi" %% "pprint" % "0.5.2",
      "com.lihaoyi" %% "ammonite-ops" % "1.0.3-32-3c3d657",
      "io.methvin" % "directory-watcher" % "0.4.0",
      ("com.lihaoyi" %% "ammonite-repl" % "1.0.3-32-3c3d657").cross(CrossVersion.full)
    ),
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "org.scalameta" %% "testkit" % "2.1.7" % Test
    ),
    buildInfoPackage := "fox.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> classDirectory.in(testsInput, Compile).value
    ),
    compile.in(Test) := compile.in(Test).dependsOn(compile.in(testsInput, Compile)).value
  )
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = project.in(file("tests/input"))
