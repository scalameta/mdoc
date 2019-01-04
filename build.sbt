inThisBuild(
  List(
    scalaVersion := "2.12.8",
    organization := "org.scalameta",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalameta/mdoc")),
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
      "com.vladsch.flexmark" % "flexmark-all" % "0.40.0",
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

val jsdocs = project
  .in(file("tests/jsdocs"))
  .settings(
    skip in publish := true,
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    libraryDependencies ++= List(
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1",
      "org.scala-js" %%% "scalajs-dom" % "0.9.6"
    ),
    scalaJSUseMainModuleInitializer := true,
    npmDependencies in Compile ++= List(
      "ms" -> "2.1.1"
    ),
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

lazy val unit = project
  .in(file("tests/unit"))
  .settings(
    skip in publish := true,
    scalacOptions ++= List(
      "-deprecation"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
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
    ),
    mdocJS := Some(jsdocs)
  )
  .dependsOn(mdoc, js, testsInput)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

lazy val plugin = project
  .in(file("mdoc-sbt"))
  .settings(
    sbtPlugin := true,
    sbtVersion in pluginCrossBuild := "1.0.0",
    moduleName := "sbt-mdoc",
    libraryDependencies ++= List(
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
      "org.scalameta" %% "testkit" % "4.0.0-M11" % Test
    ),
    resourceGenerators.in(Compile) += Def.task {
      val out =
        managedResourceDirectories.in(Compile).value.head / "sbt-mdoc.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      IO.write(props, "sbt-mdoc properties", out)
      List(out)
    },
    publishLocal := publishLocal
      .dependsOn(
        publishLocal in runtime,
        publishLocal in mdoc,
        publishLocal in js
      )
      .value,
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    )
  )
  .enablePlugins(ScriptedPlugin)

lazy val js = project
  .in(file("mdoc-js"))
  .settings(
    moduleName := "mdoc-js",
    libraryDependencies ++= List(
      "org.scala-js" % "scalajs-compiler" % "0.6.26" cross CrossVersion.full,
      "org.scala-js" %% "scalajs-tools" % "0.6.26"
    )
  )
  .dependsOn(mdoc)

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
    moduleName := "mdoc-docs",
    skip in publish := version.in(ThisBuild).value.endsWith("-SNAPSHOT"),
    mdocAutoDependency := false,
    resolvers += Resolver.bintrayRepo("cibotech", "public"),
    libraryDependencies ++= List(
      "org.scala-sbt" % "sbt" % sbtVersion.value,
      "com.cibo" %% "evilplot" % "0.6.0"
    ),
    watchSources += baseDirectory.in(ThisBuild).value / "docs",
    cancelable in Global := true,
    MdocPlugin.autoImport.mdoc := run.in(Compile).evaluated,
    mdocJS := Some(jsdocs),
    mdocJSLibraries := webpack.in(jsdocs, Compile, fullOptJS).value,
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
  .dependsOn(mdoc, js, plugin)
  .enablePlugins(DocusaurusPlugin)
