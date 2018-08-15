inThisBuild(
  List(
    scalaVersion := "2.12.6",
    organization := "com.geirsson",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/olafurpg/vork")),
    developers := List(
      Developer(
        "jvican",
        "Jorge Vicente Cantero",
        "jorgevc@fastmail.es",
        url("https://jvican.github.io/")
      ),
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      )
    )
  )
)

name := "vorkRoot"
skip in publish := true
val V = new {
  val scalameta = "4.0.0-M8"
  val scalafix = "0.6.0-M14"
}

lazy val runtime = project
  .settings(
    moduleName := "vork-runtime",
    libraryDependencies ++= List(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "pprint" % "0.5.2"
    )
  )

lazy val vork = project
  .settings(
    moduleName := "vork",
    mainClass in assembly := Some("vork.Main"),
    assemblyJarName in assembly := "vork.jar",
    test in assembly := {},
    assemblyMergeStrategy.in(assembly) ~= { old =>
      {
        case PathList("META-INF", "CHANGES") => MergeStrategy.discard
        case x => old(x)
      }
    },
    fork in run := true,
    libraryDependencies ++= List(
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % V.scalameta,
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.8.3",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "io.methvin" % "directory-watcher" % "0.4.0",
      "ch.epfl.scala" %% "scalafix-core" % V.scalafix
    ),
  )
  .dependsOn(runtime)

lazy val testsInput = project
  .in(file("tests/input"))
  .settings(
    skip in publish := true
  )

lazy val unit = project
  .in(file("tests/unit"))
  .settings(
    skip in publish := true,
    libraryDependencies ++= List(
      "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
      "org.scalatest" %% "scalatest" % "3.2.0-SNAP10" % Test,
      "org.scalameta" %% "testkit" % V.scalameta % Test
    ),
    // forking causes https://github.com/scalatest/scalatest/issues/556
    //    fork := true,
    buildInfoPackage := "vork.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> classDirectory.in(testsInput, Compile).value
    )
  )
  .dependsOn(vork, testsInput)
  .enablePlugins(BuildInfoPlugin)

lazy val website = project
  .in(file("website"))
  .settings(
    cancelable in Global := true
  )
  .dependsOn(vork)
