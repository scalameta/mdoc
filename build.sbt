def scala212 = "2.12.10"
def scala211 = "2.11.12"
def scala213 = "2.13.1"
inThisBuild(
  List(
    scalaVersion := scala212,
    crossScalaVersions := List(scala212, scala211, scala213),
    scalacOptions ++= List(
      "-Xexperimental",
      "-deprecation"
    ),
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
    resolvers += Resolver.sonatypeRepo("public"),
    // faster publishLocal:
    publishArtifact.in(packageDoc) := sys.env.contains("CI"),
    publishArtifact.in(packageSrc) := sys.env.contains("CI")
  )
)

name := "mdocRoot"
skip in publish := true
crossScalaVersions := Nil

val V = new {
  val scalameta = "4.2.5"
}

lazy val pprintVersion = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "0.5.4"
  else "0.5.5"
}

lazy val fansiVersion = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "0.2.6"
  else "0.2.7"
}

lazy val interfaces = project
  .in(file("mdoc-interfaces"))
  .settings(
    moduleName := "mdoc-interfaces",
    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled,
    javacOptions in (Compile / doc) ++= List(
      "-tag",
      "implNote:a:Implementation Note:"
    )
  )

lazy val runtime = project
  .settings(
    moduleName := "mdoc-runtime",
    libraryDependencies ++= List(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "pprint" % pprintVersion.value
    )
  )
  .dependsOn(interfaces)

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
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.9.4",
      "com.vladsch.flexmark" % "flexmark-all" % "0.40.4",
      "com.lihaoyi" %% "fansi" % fansiVersion.value,
      "io.methvin" % "directory-watcher" % "0.8.0",
      "me.xdrop" % "fuzzywuzzy" % "1.1.10", // for link hygiene "did you mean?"
      // live reload
      "io.undertow" % "undertow-core" % "2.0.13.Final",
      "org.jboss.xnio" % "xnio-nio" % "3.6.9.Final",
      "org.slf4j" % "slf4j-api" % "1.8.0-beta2"
    )
  )
  .dependsOn(runtime)
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = project
  .in(file("tests/input"))
  .settings(
    skip in publish := true
  )

val isScala213 = Def.setting {
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13"))
}

def scala212LibraryDependencies(deps: List[ModuleID]) = List(
  libraryDependencies ++= {
    if (isScala213.value) Nil
    else deps
  }
)

val jsdocs = project
  .in(file("tests/jsdocs"))
  .settings(
    skip in publish := true,
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    libraryDependencies ++= {
      if (isScala213.value) Nil
      else
        List(
          "in.nvilla" %%% "monadic-html" % "0.4.0-RC1"
        )
    },
    libraryDependencies ++= List(
      "org.scala-js" %%% "scalajs-dom" % "0.9.7"
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
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    resolvers += Resolver.bintrayRepo("cibotech", "public"),
    scala212LibraryDependencies(
      List(
        "com.cibo" %% "evilplot" % "0.6.0"
      )
    ),
    libraryDependencies ++= List(
      "co.fs2" %% "fs2-core" % "1.1.0-M1",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
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
    crossScalaVersions := List(scala212),
    moduleName := "sbt-mdoc",
    libraryDependencies ++= List(
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
      "org.scalameta" %% "testkit" % V.scalameta % Test
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
        publishLocal in interfaces,
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
    scala212LibraryDependencies(
      List(
        "org.scala-js" % "scalajs-compiler" % "0.6.28" cross CrossVersion.full,
        "org.scala-js" %% "scalajs-tools" % "0.6.28"
      )
    )
  )
  .dependsOn(mdoc)

lazy val lsp = project
  .in(file("mdoc-lsp"))
  .settings(
    moduleName := "mdoc-lsp",
    crossScalaVersions := List(scala212),
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
    crossScalaVersions := List(scala212),
    skip in publish :=
      !scalaVersion.value.startsWith("2.12") ||
        version.in(ThisBuild).value.endsWith("-SNAPSHOT"),
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
