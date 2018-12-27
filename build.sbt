inThisBuild(
  List(
    scalaVersion := "2.12.8",
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
    ),
    // faster publishLocal:
    publishArtifact.in(packageDoc) := sys.env.contains("CI"),
    publishArtifact.in(packageSrc) := sys.env.contains("CI")
  )
)

name := "mdocRoot"
skip in publish := true
val V = new {
  val scalameta = "4.1.0"
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
      version,
      scalaVersion,
      scalaBinaryVersion
    ),
    libraryDependencies ++= List(
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % V.scalameta,
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.9.1",
      "com.vladsch.flexmark" % "flexmark-all" % "0.34.44",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "io.methvin" % "directory-watcher" % "0.8.0",
      "me.xdrop" % "fuzzywuzzy" % "1.1.10", // for link hygiene "did you mean?"
      // live reload
      "io.undertow" % "undertow-core" % "2.0.13.Final",
      "org.jboss.xnio" % "xnio-nio" % "3.6.5.Final",
      "org.slf4j" % "slf4j-nop" % "1.8.0-beta2"
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
    scalacOptions ++= List(
      "-deprecation"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
    resourceGenerators.in(Test) += Def.task {
      val out = managedResourceDirectories.in(Test).value.head / "mdoc.properties"
      val props = new java.util.Properties()
      props.put("scalacOptions", scalacOptions.in(Compile).value.mkString(" "))
      IO.write(props, "mdoc properties", out)
      List(out)
    },
    resolvers += Resolver.bintrayRepo("cibotech", "public"),
    libraryDependencies ++= List(
      "com.cibo" %% "evilplot" % "0.6.0",
      "co.fs2" %% "fs2-core" % "0.10.4",
      "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
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

lazy val plugin = project
  .in(file("mdoc-sbt"))
  .settings(
    sbtPlugin := true,
    moduleName := "sbt-mdoc"
  )

lazy val lsp = project
  .in(file("mdoc-lsp"))
  .settings(
    moduleName := "mdoc-lsp",
    libraryDependencies ++= List(
      "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.5.0",
      "com.outr" %% "scribe" % "2.6.0",
      "com.outr" %% "scribe-slf4j" % "2.6.0"
    )
  )
  .dependsOn(mdoc)

lazy val docs = project
  .in(file("mdoc-docs"))
  .settings(
    skip in publish := true,
    resolvers += Resolver.bintrayRepo("cibotech", "public"),
    libraryDependencies ++= List(
      "com.cibo" %% "evilplot" % "0.6.0"
    ),
    test := run.in(Compile).toTask(" --test").value,
    watchSources += baseDirectory.in(ThisBuild).value / "docs",
    cancelable in Global := true,
    mdocVariables := {
      val stableVersion: String =
        version.value.replaceFirst("\\+.*", "")
      Map(
        "VERSION" -> stableVersion,
        "SCALA_BINARY_VERSION" -> scalaBinaryVersion.value,
        "SCALA_VERSION" -> scalaVersion.value
      )
    }
  )
  .dependsOn(mdoc)
  .enablePlugins(MdocPlugin)
