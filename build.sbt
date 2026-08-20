import Extensions._

import scala.collection.mutable
import scala.scalanative.build._

Global / resolvers += "scala-nightlies" at
  "https://repo.scala-lang.org/artifactory/maven-nightlies"

def rowsAt(task: String, v: String, extraJvm: Matrix*) = {
  val jvm = Iterable(runtime, cli, mdoc, testsInput, tests, worksheets, unit, unitJS, js, jsWorker)
  onEach(task, v, jvm ++ extraJvm, Iterable(jsdocs, jswebsitedocs))
}

addCommandAlias("testAllNonNative", rowsAt("testFull", scala213, parser))
addCommandAlias("test212", rowsAt("testFull", scala212))
addCommandAlias("test213", rowsAt("testFull", scala213))
addCommandAlias("test33", rowsAt("testFull", scala3))
// the next Scala is tested where the 3.8.4 job tested it
addCommandAlias("test38", onEach("testFull", scala3next, List(unit, worksheets), Nil))

def scalajsBinaryVersion = "1"
def scalajsDom = "2.0.0"

def isCI = System.getenv("CI") != null

def jsoniter = List("core", "macros").map { pkg =>
  "com.github.plokhotnyuk.jsoniter-scala" %% s"jsoniter-scala-$pkg" % "2.40.1"
}

def multiScalaDirectories(projectName: String) =
  Def.setting {
    val base = srcWithRoot((ThisBuild / baseDirectory).value, projectName, "main")
    def path(ver: String) = base / s"scala-$ver"
    val paths = path(scalaVersion.value) :: path(if (isScala3.value) "3" else "2") :: Nil
    val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
    partialVersion.collect { case (major, minor) => path(s"$major.$minor") }.fold(paths)(_ :: paths)
  }

def crossSetting[A](
    scalaVersion: String,
    if2: List[A] = Nil,
    if3: List[A] = Nil,
    if212: List[A] = Nil,
    if213: List[A] = Nil
): List[A] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) => minor match {
        case 12 => if2 ::: if212
        case 13 => if2 ::: if213
        case _ => if2
      }
    case Some((3, _)) => if3
    case _ => Nil
  }

inThisBuild(
  List(
    // version is set dynamically by sbt-dynver, but let's adjust it
    version := {
      val curVersion = version.value
      def dynVer(out: sbtdynver.GitDescribeOutput): String = {
        def tagVersion = out.ref.dropPrefix
        if (out.isCleanAfterTag) tagVersion
        else if (System.getenv("CI") == null)
          s"$tagVersion-next-SNAPSHOT" // modified for local builds
        else if (out.commitSuffix.distance == 0) tagVersion
        else if (sys.props.contains("backport.release")) tagVersion
        else curVersion
      }
      dynverGitDescribeOutput.value.mkVersion(dynVer, curVersion)
    },
    scalaVersion := scala213,
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
    resolvers += Resolver.sonatypeCentralSnapshots,
    // faster publishLocal:
    packageDoc / publishArtifact := isCI,
    packageSrc / publishArtifact := isCI,
    turbo := true,
    useSuperShell := false // overlaps with MUnit test failure reports.
  )
)

LocalRootProject / name := "mdocRoot"
LocalRootProject / publish / skip := true
LocalRootProject / crossScalaVersions := Nil

lazy val sharedSettings = List(
  // mdoc-interfaces is Java, so its row carries whichever Scala version the build defaults to
  allowMismatchScala := true,
  scalacOptions ++= crossSetting(
    scalaVersion.value,
    if2 = List("-Yrangepos", "-deprecation"),
    if213 = List("-release", "11"),
    if212 = List("-Xexperimental", "-release", "8"),
    if3 = List("-language:implicitConversions", "-Ximport-suggestion-timeout", "0")
  )
)

lazy val sharedJavaSettings = List(
  javacOptions ++= Seq("--release", "11")
)

lazy val depCoursierInterfaces = Def.settings(
  libraryDependencies += "io.get-coursier" % "interface" % "1.0.28"
)

lazy val interfaces = project
  .in(file("mdoc-interfaces"))
  .settings(
    moduleName := "mdoc-interfaces",
    autoScalaLibrary := false,
    depCoursierInterfaces,
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
    ),
    javacOptions ++= Seq("--release", "8")
  )

lazy val runtime = projectMatrix.allJvm()
  .settings(
    sharedSettings,
    moduleName := "mdoc-runtime",
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("runtime").value,
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if2 = List(
        "com.lihaoyi" %% "pprint" % V.pprint,
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided
      ),
      if3 = List(
        "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
      )
    )
  )
  .dependsOn(interfaces)

lazy val depScalameta = Def.settings(
  libraryDependencies += {
    val sm = ("org.scalameta" %% "scalameta" % V.scalameta).excludeAll("com.lihaoyi")
    if (isScala3.value)
      sm.excludeAll(
        "org.scala-lang.modules" % s"scala-collection-compat_${scalaBinaryVersion.value}"
      ).cross(CrossVersion.for3Use2_13)
    else sm
  }
)

lazy val parser = projectMatrix.crossAll
  .settings(
    sharedSettings,
    moduleName := "mdoc-parser"
  )

lazy val cli = projectMatrix.allJvm()
  .settings(
    sharedSettings,
    moduleName := "mdoc-cli",
    depCoursierInterfaces,
    libraryDependencies ++= List(
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "com.lihaoyi" %% "pprint" % V.pprint,
      "org.scalameta" %% "metaconfig-typesafe-config" % V.metaconfig
    ),
    depScalameta
  )
  .dependsOn(parser)

lazy val mdoc = projectMatrix.allJvm()
  .settings(
    sharedSettings,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("mdoc").value,
    moduleName := "mdoc",
    Compile / mainClass := Some("mdoc.Main"),
    run / fork := true,
    buildInfoPackage := "mdoc.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion,
      scalaBinaryVersion
    ),
    depScalameta,
    libraryDependencies += {
      if (isScala3.value)
        "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
      else
        "org.scala-lang" % "scala-compiler" % scalaVersion.value
    },
    libraryDependencies ++= jsoniter,
    libraryDependencies ++= List(
      "org.virtuslab" % "using_directives" % "1.1.4",
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
      "io.methvin" % "directory-watcher" % "0.19.1",
      // live reload
      "io.undertow" % "undertow-core" % "2.2.30.Final",
      "org.jboss.xnio" % "xnio-nio" % "3.8.17.Final",
      "org.slf4j" % "slf4j-api" % "2.0.18",
      "org.scalameta" %% "metaconfig-typesafe-config" % V.metaconfig,
      "com.lihaoyi" %% "fansi" % V.fansi,
      "com.lihaoyi" %% "pprint" % V.pprint
    )
  )
  .dependsOn(parser, runtime, cli)
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = projectMatrix.allJvm()
  .in(file("tests/input"))
  .settings(
    sharedSettings,
    unpublished
  )

def scala212LibraryDependencies(deps: List[ModuleID]) =
  List(
    libraryDependencies ++= {
      if (isScala212.value) deps
      else Nil
    }
  )
val tests = projectMatrix.allJvm()
  .in(file("tests/tests"))
  .settings(
    sharedSettings,
    unpublished,
    libraryDependencies += depMunit,
    buildInfoPackage := "tests",
    buildInfoKeys := Seq[BuildInfoKey](
      scalaVersion,
      scalaBinaryVersion
    )
  )
  .enablePlugins(BuildInfoPlugin)

val jsdocs = projectMatrix.jsPlatform(allScalaVersions)
  .in(file("tests/jsdocs"))
  .settings(
    sharedSettings,
    unpublished,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    depJsDom,
    scalaJSUseMainModuleInitializer := true
  )
  .enablePlugins(ScalaJSPlugin)

val jswebsitedocs = projectMatrix.jsPlatform(allScalaVersions)
  .in(file("tests/websiteJs"))
  .settings(
    sharedSettings,
    unpublished,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    depJsDom
  )
  .enablePlugins(ScalaJSPlugin)

// a forked JVM takes the platform charset before Java 18, and the tests read UTF-8 sources
def forkedTests = Def.settings(
  Test / fork := true,
  Test / javaOptions += "-Dfile.encoding=UTF-8"
)

lazy val worksheets = projectMatrix.allJvm()
  .in(file("tests/worksheets"))
  .settings(
    sharedSettings,
    unpublished,
    forkedTests,
    libraryDependencies += depMunit % Test
  )
  .dependsOn(mdoc, tests)

def unitRow(v: String) = buildInfoKeys := Seq[BuildInfoKey](
  "testsInputClassDirectory" -> (testsInput.jvm(v) / Compile / classDirectory).value
)

lazy val unit = projectMatrix
  .in(file("tests/unit"))
  .settings(
    sharedSettings,
    unpublished,
    forkedTests,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("tests/unit").value,
    libraryDependencies ++= {
      if (isScala3.value) List()
      else List(compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"))
    },
    scala212LibraryDependencies(
      List(
        "io.github.cibotech" %% "evilplot" % "0.9.2"
      )
    ),
    libraryDependencies += depMunit % Test,
    libraryDependencies += {
      val dep = "co.fs2" %% "fs2-core" % V.fs2
      if (isScala3.value) dep.cross(CrossVersion.for3Use2_13) else dep
    },
    buildInfoPackage := "tests.cli"
  )
  .allJvmRows(unitRow)
  .dependsOn(parser, mdoc, testsInput, tests)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

// products returns the output directories, and compiles the worker first
def jsWorkerClasspath(v: String) = {
  val cfg = jsWorker.jvm(v) / Compile
  MdocPlugin.mdocJSWorkerClasspath :=
    Some((cfg / products).value ++ (cfg / resourceDirectories).value)
}

def unitJSRow(v: String) = Def.settings(
  mdocJS := Some(jsdocs.js(v)),
  // unit ships an mdoc.properties of its own, and it comes first on the test classpath
  mdocPropertiesPrefix := "js-",
  unitRow(v),
  jsWorkerClasspath(v)
)

lazy val unitJS = projectMatrix
  .in(file("tests/unit-js"))
  .settings(
    sharedSettings,
    unpublished,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("tests/unit-js").value,
    libraryDependencies += depMunit % Test,
    buildInfoPackage := "tests.js"
  )
  .jvmRows(allScalaVersions)(unitJSRow)
  .dependsOn(mdoc, js, testsInput, tests, unit)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

lazy val plugin = project
  .in(file("mdoc-sbt"))
  .settings(
    sharedSettings,
    sbtPlugin := true,
    scalaVersion := scala212,
    // the floor a user of this plugin must be on, not the sbt we build with
    pluginCrossBuild / sbtVersion :=
      (scalaBinaryVersion.value match {
        case "2.12" => "1.5.0"
        case _ => "2.0.0"
      }),
    crossScalaVersions := List(scala212, scala3next),
    moduleName := "sbt-mdoc",
    libraryDependencies ++= List(
      "org.jsoup" % "jsoup" % "1.23.1",
      "org.scalacheck" %% "scalacheck" % V.scalacheck % Test,
      depMunit % Test,
      "org.scalameta" %% "testkit" % V.scalameta % Test
    ),
    Compile / resourceGenerators += Def.task {
      val out = (Compile / managedResourceDirectories).value.head / "sbt-mdoc.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      props.put("scalaJSVersion", scalaJSVersion)
      IO.write(props, "sbt-mdoc properties", out)
      Seq(out)
    },
    publishLocal := {
      publishLocal
        .dependsOn(
          (interfaces / publishLocal)
            .dependsOn(jsApi / publishLocal)
            .dependsOn(localCrossPublish(List(scala212, scala213, scala3)))
        )
        .value
    },
    scriptedBufferLog := false,
    // the sbt that runs the scripted tests, not the floor a user must be on: sbt 1.5.0 ships
    // Scala 2.12.13, which cannot read the classfiles of a current JDK
    scriptedSbt :=
      (scalaBinaryVersion.value match {
        case "2.12" => "1.12.13"
        case _ => (pluginCrossBuild / sbtVersion).value
      }),
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    )
  )
  .enablePlugins(ScriptedPlugin)

lazy val jsApi =
  project
    .in(file("mdoc-js-interfaces"))
    .settings(moduleName := "mdoc-js-interfaces", crossPaths := false, autoScalaLibrary := false)
    .settings(sharedJavaSettings)

lazy val jsWorker =
  projectMatrix.jvmPlatform(allScalaVersions)
    .in(file("mdoc-js-worker"))
    .dependsOn(jsApi)
    .settings(
      sharedSettings,
      moduleName := "mdoc-js-worker",
      libraryDependencies ++= Seq(
        "org.scala-js" %% "scalajs-linker" % scalaJSVersion % Provided cross
          CrossVersion.for3Use2_13,
        "com.armanbilge" %% "scalajs-importmap" % "0.1.1" cross CrossVersion.for3Use2_13
      )
    )

lazy val js = projectMatrix.jvmPlatform(allScalaVersions)
  .in(file("mdoc-js"))
  .dependsOn(jsApi)
  .settings(
    sharedSettings,
    moduleName := "mdoc-js",
    libraryDependencies ++= jsoniter,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("js").value
  )
  .dependsOn(mdoc)

lazy val docs = project
  .in(file("mdoc-docs"))
  .settings(
    sharedSettings,
    moduleName := "mdoc-docs",
    unpublished,
    docusaurusVersion := DocusaurusVersion.V1,
    scalaVersion := scala212,
    crossScalaVersions := List(scala212),
    mdocAutoDependency := false,
    libraryDependencies ++= List(
      // the sbt the plugin is built against, not the one running this build
      "org.scala-sbt" % "sbt" % (plugin / pluginCrossBuild / sbtVersion).value,
      "io.github.cibotech" %% "evilplot" % "0.9.2"
    ),
    watchSources := Def.uncached(
      watchSources.value :+ WatchSource((ThisBuild / baseDirectory).value / "docs")
    ),
    Global / cancelable := true,
    MdocPlugin.autoImport.mdoc := (Compile / run).evaluated,
    mdocJS := Some(jswebsitedocs.js(scala212)),
    jsWorkerClasspath(scala212),
    dependencyOverrides += {
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0"
    },
    mdocVariables := {
      val stableVersion: String =
        version.value.replaceFirst("\\+.*", "")
      Map(
        "VERSION" -> stableVersion,
        "SCALA_BINARY_VERSION" -> scalaBinaryVersion.value,
        "SCALA_VERSION" -> scalaVersion.value,
        "SCALAJS_VERSION" -> scalaJSVersion,
        "SCALAJS_BINARY_VERSION" -> scalajsBinaryVersion,
        "SCALAJS_DOM_VERSION" -> scalajsDom
      )
    }
  )
  .dependsOn(mdoc.jvm(scala212), js.jvm(scala212), plugin)
  .enablePlugins(DocusaurusPlugin)

// a row per version exists now, so publishing them needs no session surgery
def localCrossPublish(versions: List[String]): Def.Initialize[Task[Unit]] = {
  val all = Seq(parser)
  val jvm = Seq(runtime, cli, mdoc, js, jsWorker)
  versions
    .flatMap { v =>
      all.flatMap(x => Seq(x.jvm(v), x.js(v), x.native(v))) ++ jvm.map(x => x.jvm(v))
    }
    .map(p => (p / publishLocal).map(_ => ()))
    .reduceLeft((a, b) => a.dependsOn(b))
}

val depMunit = "org.scalameta" %% "munit" % V.munit

lazy val depJsDom = Def.settings(
  libraryDependencies += "org.scala-js" %% "scalajs-dom" % scalajsDom
)
