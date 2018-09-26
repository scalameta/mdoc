inThisBuild(
  List(
    scalaVersion := "2.12.6",
    organization := "com.geirsson",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/olafurpg/mdoc")),
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

name := "mdocRoot"
skip in publish := true
val V = new {
  val scalameta = "4.0.0-M8"
  val scalafix = "0.6.0-M14"
}

lazy val runtime = project
  .settings(
    moduleName := "mdoc-runtime",
    libraryDependencies ++= List(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "pprint" % "0.5.2"
    )
  )

lazy val mdoc = project
  .settings(
    moduleName := "mdoc",
    crossVersion := CrossVersion.full,
    mainClass in assembly := Some("mdoc.Main"),
    assemblyJarName in assembly := "mdoc.jar",
    test in assembly := {},
    assemblyMergeStrategy.in(assembly) ~= { old =>
      {
        case PathList("META-INF", "CHANGES") => MergeStrategy.discard
        case x => old(x)
      }
    },
    fork in run := true,
    buildInfoPackage := "mdoc.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version
    ),
    libraryDependencies ++= List(
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % V.scalameta,
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.8.3",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "io.methvin" % "directory-watcher" % "0.7.0",
      "me.xdrop" % "fuzzywuzzy" % "1.1.9", // for link hygiene "did you mean?"
      "ch.epfl.scala" %% "scalafix-core" % V.scalafix
    )
  )
  .dependsOn(runtime)
  .enablePlugins(BuildInfoPlugin)

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
    buildInfoPackage := "tests",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> classDirectory.in(testsInput, Compile).value
    )
  )
  .dependsOn(mdoc, testsInput)
  .enablePlugins(BuildInfoPlugin)

lazy val docs = project
  .in(file("mdoc-docs"))
  .settings(
    skip in publish := true,
    test := run.in(Compile).toTask(" --test").value,
    watchSources += baseDirectory.in(ThisBuild).value / "docs",
    cancelable in Global := true,
  )
  .dependsOn(mdoc)
