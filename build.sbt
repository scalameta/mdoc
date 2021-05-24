import scala.collection.mutable

def scala212 = "2.12.13"
def scala211 = "2.11.12"
def scala213 = "2.13.5"
def scala3 = List("3.0.0", "3.0.0-RC3", "3.0.0-RC2", "3.0.0-RC1")
def scala2Versions = List(scala212, scala211, scala213)
def allScalaVersions = scala2Versions ::: scala3

def scalajs = "1.5.0"
def scalajsBinaryVersion = "1"
def scalajsDom = "1.1.0"

def isScala2(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2)
def isScala212(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2) && v.exists(_._2 == 12)
def isScala211(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2) && v.exists(_._2 == 11)
def isScala3(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 3)

val isScala212 = Def.setting {
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("2.12.x"))
}

val isScala3 = Def.setting {
  // doesn't work well with >= 3.0.0 for `3.0.0-M1`
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("<=1.0.0 || >=2.99.0"))
}

val isScalaJs1 = Def.setting {
  VersionNumber(scalaJSVersion).matchesSemVer(SemanticSelector(">=1.0.0"))
}

def multiScalaDirectories(projectName: String) =
  Def.setting {
    val root = (ThisBuild / baseDirectory).value / projectName
    val base = root / "src" / "main"
    val result = mutable.ListBuffer.empty[File]
    val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
    partialVersion.collect { case (major, minor) =>
      result += base / s"scala-$major.$minor"
    }

    result += base / s"scala-${scalaVersion.value}"
    if (isScala3.value) {
      result += base / "scala-3"
    }

    if (!isScala3.value) {
      result += base / "scala-2"
    }
    result.toList
  }

def crossSetting[A](
    scalaVersion: String,
    if2: List[A] = Nil,
    if3: List[A] = Nil,
    if211: List[A] = Nil,
    if212: List[A] = Nil
): List[A] =
  CrossVersion.partialVersion(scalaVersion) match {
    case partialVersion if isScala2(partialVersion) => if2
    case partialVersion if isScala3(partialVersion) => if3
    case partialVersion if isScala211(partialVersion) => if2 ::: if211
    case partialVersion if isScala212(partialVersion) => if2 ::: if212
    case _ => Nil
  }

inThisBuild(
  List(
    scalaVersion := scala213,
    crossScalaVersions := scala2Versions ::: scala3,
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
    packageDoc / publishArtifact := "true" == System.getenv("CI"),
    packageSrc / publishArtifact := "true" == System.getenv("CI"),
    turbo := true,
    useSuperShell := false // overlaps with MUnit test failure reports.
  )
)

name := "mdocRoot"
publish / skip := true
crossScalaVersions := Nil
lazy val sharedSettings = List(
  scalacOptions ++= crossSetting(
    scalaVersion.value,
    if2 = List("-target:jvm-1.8", "-Yrangepos", "-deprecation"),
    if212 = List("-Xexperimental"),
    if211 = List("-Xexperimental"),
    if3 = List("-language:implicitConversions", "-Ximport-suggestion-timeout", "0")
  )
)

val V = new {
  val scalameta = "4.4.14"
  val munit = "0.7.26"
  val coursier = "1.0.3"
  val scalacheck = "1.15.4"
}

lazy val pprintVersion = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "0.5.4"
  else "0.6.0"
}

lazy val fansiVersion = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "0.2.6"
  else "0.2.9"
}

lazy val fs2Version = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "2.1.0"
  else if (scalaVersion.value == "3.0.0-RC1") "2.5.4"
  else "2.5.5"
}

lazy val interfaces = project
  .in(file("mdoc-interfaces"))
  .settings(
    moduleName := "mdoc-interfaces",
    autoScalaLibrary := false,
    libraryDependencies ++= List(
      "io.get-coursier" % "interface" % V.coursier
    ),
    // @note needed to deal with issues with dottyDoc
    Compile / doc / sources := {
      if (isScala3.value) {
        Seq.empty
      } else {
        (Compile / doc / sources).value
      }
    },
    crossVersion := CrossVersion.disabled,
    Compile / doc / javacOptions ++= List(
      "-tag",
      "implNote:a:Implementation Note:"
    )
  )

lazy val runtime = project
  .settings(
    sharedSettings,
    moduleName := "mdoc-runtime",
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("runtime").value,
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if2 = List(
        "com.lihaoyi" %% "pprint" % pprintVersion.value,
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided
      ),
      if3 = List(
        "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
      )
    )
  )
  .dependsOn(interfaces)

val excludePprint = ExclusionRule(organization = "com.lihaoyi")

lazy val cli = project
  .settings(
    sharedSettings,
    moduleName := "mdoc-cli",
    scalaVersion := scala213,
    crossScalaVersions := scala2Versions,
    libraryDependencies ++= List(
      "io.get-coursier" % "interface" % V.coursier,
      "com.vladsch.flexmark" % "flexmark-all" % "0.62.2",
      "org.scalameta" %% "scalameta" % V.scalameta,
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.9.10"
    )
  )

lazy val mdoc = project
  .settings(
    sharedSettings,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("mdoc").value,
    moduleName := "mdoc",
    assembly / mainClass := Some("mdoc.Main"),
    assembly / assemblyJarName := "mdoc.jar",
    assembly / test := {},
    assembly / assemblyMergeStrategy ~= { old =>
      {
        case PathList("META-INF", "CHANGES") => MergeStrategy.discard
        case x => old(x)
      }
    },
    run / fork := true,
    buildInfoPackage := "mdoc.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion,
      scalaBinaryVersion
    ),
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if3 = List(
        "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
        ("org.scalameta" %% "scalameta" % V.scalameta)
          .excludeAll(excludePprint)
          .cross(CrossVersion.for3Use2_13)
      ),
      if2 = List(
        "org.scala-lang" % "scala-compiler" % scalaVersion.value,
        "org.scalameta" %% "scalameta" % V.scalameta,
        "com.geirsson" %% "metaconfig-typesafe-config" % "0.9.10",
        "com.lihaoyi" %% "fansi" % fansiVersion.value
      )
    ),
    libraryDependencies ++= List(
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
      "io.methvin" % "directory-watcher" % "0.15.0",
      // live reload
      "io.undertow" % "undertow-core" % "2.2.7.Final",
      "org.jboss.xnio" % "xnio-nio" % "3.8.4.Final",
      "org.slf4j" % "slf4j-api" % "1.7.30"
    )
  )
  .dependsOn(runtime, cli)
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = project
  .in(file("tests/input"))
  .settings(
    sharedSettings,
    publish / skip := true
  )

def scala212LibraryDependencies(deps: List[ModuleID]) =
  List(
    libraryDependencies ++= {
      if (isScala212.value) deps
      else Nil
    }
  )
val tests = project
  .in(file("tests/tests"))
  .settings(
    sharedSettings,
    publish / skip := true,
    libraryDependencies ++= List(
      "org.scalameta" %% "munit" % V.munit
    ),
    buildInfoPackage := "tests",
    buildInfoKeys := Seq[BuildInfoKey](
      scalaVersion,
      scalaBinaryVersion
    )
  )
  .enablePlugins(BuildInfoPlugin)

val jsdocs = project
  .in(file("tests/jsdocs"))
  .settings(
    sharedSettings,
    publish / skip := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    libraryDependencies ++= List(
      "org.scala-js" %%% "scalajs-dom" % scalajsDom cross CrossVersion.for3Use2_13
    ),
    scalaJSUseMainModuleInitializer := true,
    Compile / npmDependencies ++= List(
      "ms" -> "2.1.1"
    ),
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

lazy val worksheets = project
  .in(file("tests/worksheets"))
  .settings(
    sharedSettings,
    publish / skip := true,
    libraryDependencies ++= List(
      "org.scalameta" %% "munit" % V.munit % Test
    )
  )
  .dependsOn(mdoc, tests)

lazy val unit = project
  .in(file("tests/unit"))
  .settings(
    sharedSettings,
    publish / skip := true,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("tests/unit").value,
    libraryDependencies ++= {
      if (isScala3.value) List()
      else List(compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"))
    },
    scala212LibraryDependencies(
      List(
        "io.github.cibotech" %% "evilplot" % "0.8.1"
      )
    ),
    libraryDependencies ++= List(
      "org.scalameta" %% "munit" % V.munit % Test
    ),
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if3 = List(
        ("co.fs2" %% "fs2-core" % fs2Version.value)
          .cross(CrossVersion.for3Use2_13)
      ),
      if2 = List(
        "co.fs2" %% "fs2-core" % fs2Version.value
      )
    ),
    buildInfoPackage := "tests.cli",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> (testsInput / Compile / classDirectory).value
    )
  )
  .dependsOn(mdoc, testsInput, tests)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

lazy val unitJS = project
  .in(file("tests/unit-js"))
  .settings(
    sharedSettings,
    publish / skip := true,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("tests/unit-js").value,
    libraryDependencies ++= List(
      "org.scalameta" %% "munit" % V.munit % Test
    ),
    buildInfoPackage := "tests.js",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> (testsInput / Compile / classDirectory).value
    ),
    mdocJS := Some(jsdocs)
  )
  .dependsOn(mdoc, js, testsInput, tests, unit)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

lazy val plugin = project
  .in(file("mdoc-sbt"))
  .settings(
    sharedSettings,
    sbtPlugin := true,
    scalaVersion := scala212,
    pluginCrossBuild / sbtVersion := "1.0.0",
    crossScalaVersions := List(scala212),
    moduleName := "sbt-mdoc",
    libraryDependencies ++= List(
      "org.jsoup" % "jsoup" % "1.12.1",
      "org.scalacheck" %% "scalacheck" % V.scalacheck % Test,
      "org.scalameta" %% "munit" % V.munit % Test,
      "org.scalameta" %% "testkit" % V.scalameta % Test
    ),
    Compile / resourceGenerators += Def.task {
      val out =
        (Compile / managedResourceDirectories).value.head / "sbt-mdoc.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      IO.write(props, "sbt-mdoc properties", out)
      List(out)
    },
    publishLocal := publishLocal
      .dependsOn(
        interfaces / publishLocal,
        runtime / publishLocal,
        mdoc / publishLocal,
        cli / publishLocal,
        js / publishLocal
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
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("js").value,
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if2 = List(
        "org.scala-js" % "scalajs-compiler" % scalajs cross CrossVersion.full,
        "org.scala-js" %% "scalajs-linker" % scalajs
      ),
      if3 = List(
        "org.scala-js" %% "scalajs-linker" % scalajs cross CrossVersion.for3Use2_13
      )
    )
  )
  .dependsOn(mdoc)

lazy val docs = project
  .in(file("mdoc-docs"))
  .settings(
    sharedSettings,
    moduleName := "mdoc-docs",
    scalaVersion := scala212,
    crossScalaVersions := List(scala212),
    publish / skip :=
      !scalaVersion.value.startsWith("2.12") ||
        (ThisBuild / version).value.endsWith("-SNAPSHOT"),
    mdocAutoDependency := false,
    libraryDependencies ++= List(
      "org.scala-sbt" % "sbt" % sbtVersion.value,
      "io.github.cibotech" %% "evilplot" % "0.8.1"
    ),
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    Global / cancelable := true,
    MdocPlugin.autoImport.mdoc := (Compile / run).evaluated,
    mdocJS := Some(jsdocs),
    mdocJSLibraries := (jsdocs / Compile / fullOptJS / webpack).value,
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
