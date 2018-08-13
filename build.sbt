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

lazy val runtime = project
  .settings(
    libraryDependencies ++= List(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "pprint" % "0.5.2"
    )
  )

lazy val vork = project
  .settings(
    mainClass in assembly := Some("vork.Cli"),
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
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % "3.2.0",
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.8.3",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "io.methvin" % "directory-watcher" % "0.4.0",
      "ch.epfl.scala" %% "scalafix-core" % "0.5.9"
    ),
  )
  .dependsOn(runtime)

lazy val testsInput = project.in(file("tests/input"))

lazy val unit = project
  .in(file("tests/unit"))
  .settings(
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "org.scalameta" %% "testkit" % "2.1.7" % Test
    ),
    fork := true,
    buildInfoPackage := "vork.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> classDirectory.in(testsInput, Compile).value
    )
  )
  .dependsOn(vork, testsInput)
  .enablePlugins(BuildInfoPlugin)
