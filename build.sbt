def scala212 = "2.12.11"
def scala211 = "2.11.12"
def scala213 = "2.13.2"
def scalajs = "0.6.32"
def scalajsBinaryVersion = "0.6"
def scalajsDom = "1.0.0"
inThisBuild(
  List(
    scalaVersion := scala212,
    crossScalaVersions := List(scala212, scala211, scala213),
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
    testFrameworks := List(new TestFramework("munit.Framework")),
    resolvers += Resolver.sonatypeRepo("public"),
    // faster publishLocal:
    publishArtifact.in(packageDoc) := "true" == System.getenv("CI"),
    publishArtifact.in(packageSrc) := "true" == System.getenv("CI"),
    turbo := true,
    useSuperShell := false // overlaps with MUnit test failure reports.
  )
)

name := "mdocRoot"
skip in publish := true
crossScalaVersions := Nil
lazy val sharedSettings = List(
  scalacOptions ++= {
    val buf = collection.mutable.ListBuffer.empty[String]
    buf ++= List("-target:jvm-1.8", "-Yrangepos", "-deprecation")
    if (!scalaBinaryVersion.value.startsWith("2.13"))
      buf += "-Xexperimental"
    buf.toList
  }
)

val V = new {
  val scalameta = "4.3.13"
  val munit = "0.7.1"
  val coursier = "0.0.22"
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
    libraryDependencies ++= List(
      "io.get-coursier" % "interface" % V.coursier
    ),
    crossVersion := CrossVersion.disabled,
    javacOptions in (Compile / doc) ++= List(
      "-tag",
      "implNote:a:Implementation Note:"
    )
  )

lazy val runtime = project
  .settings(
    sharedSettings,
    moduleName := "mdoc-runtime",
    libraryDependencies ++= List(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "pprint" % pprintVersion.value
    )
  )
  .dependsOn(interfaces)

lazy val mdoc = project
  .settings(
    sharedSettings,
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
      "io.get-coursier" % "interface" % V.coursier,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % V.scalameta,
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.9.10",
      "com.vladsch.flexmark" % "flexmark-all" % "0.40.34",
      "com.lihaoyi" %% "fansi" % fansiVersion.value,
      "io.methvin" % "directory-watcher" % "0.9.9",
      "me.xdrop" % "fuzzywuzzy" % "1.2.0", // for link hygiene "did you mean?"
      // live reload
      "io.undertow" % "undertow-core" % "2.1.0.Final",
      "org.jboss.xnio" % "xnio-nio" % "3.8.0.Final",
      "org.slf4j" % "slf4j-api" % "1.7.30"
    )
  )
  .dependsOn(runtime)
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = project
  .in(file("tests/input"))
  .settings(
    sharedSettings,
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
    sharedSettings,
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
      "org.scala-js" %%% "scalajs-dom" % scalajsDom
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
    sharedSettings,
    skip in publish := true,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    resolvers += Resolver.bintrayRepo("cibotech", "public"),
    scala212LibraryDependencies(
      List(
        "com.cibo" %% "evilplot" % "0.6.3"
      )
    ),
    libraryDependencies ++= List(
      "co.fs2" %% "fs2-core" % "2.1.0",
      "org.scalacheck" %% "scalacheck" % "1.14.3" % Test,
      "org.scalameta" %% "munit" % V.munit % Test,
      "org.scalameta" %% "testkit" % V.scalameta % Test
    ),
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
    sharedSettings,
    sbtPlugin := true,
    sbtVersion in pluginCrossBuild := "1.0.0",
    crossScalaVersions := List(scala212),
    moduleName := "sbt-mdoc",
    libraryDependencies ++= List(
      "org.jsoup" % "jsoup" % "1.12.1",
      "org.scalacheck" %% "scalacheck" % "1.14.3" % Test,
      "org.scalameta" %% "munit" % V.munit % Test,
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
    sharedSettings,
    moduleName := "mdoc-js",
    scala212LibraryDependencies(
      List(
        "org.scala-js" % "scalajs-compiler" % scalajs cross CrossVersion.full,
        "org.scala-js" %% "scalajs-tools" % scalajs
      )
    )
  )
  .dependsOn(mdoc)

lazy val docs = project
  .in(file("mdoc-docs"))
  .settings(
    sharedSettings,
    moduleName := "mdoc-docs",
    crossScalaVersions := List(scala212),
    skip in publish :=
      !scalaVersion.value.startsWith("2.12") ||
        version.in(ThisBuild).value.endsWith("-SNAPSHOT"),
    mdocAutoDependency := false,
    resolvers += Resolver.bintrayRepo("cibotech", "public"),
    libraryDependencies ++= List(
      "org.scala-sbt" % "sbt" % sbtVersion.value,
      "com.cibo" %% "evilplot" % "0.6.3"
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
        "SCALA_VERSION" -> scalaVersion.value,
        "SCALAJS_VERSION" -> scalajs,
        "SCALAJS_BINARY_VERSION" -> scalajsBinaryVersion,
        "SCALAJS_DOM_VERSION" -> scalajsDom
      )
    }
  )
  .dependsOn(mdoc, js, plugin)
  .enablePlugins(DocusaurusPlugin)
