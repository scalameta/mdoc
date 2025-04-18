import scala.collection.mutable
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import scala.scalanative.build._

addCommandAlias(
  "testAllNonNative",
  "interfaces/test;runtime/test;parser/test;cli/test;mdoc/test;testsInput/test;tests/test;jsdocs/test;worksheets/test;unit/test;unitJS/test;jsApi/test;jsWorker/test;js/test;"
)

def scala212 = "2.12.20"
def scala213 = "2.13.16"
def scala3 = "3.3.5"
def scala2Versions = List(scala212, scala213)
def allScalaVersions = scala2Versions :+ scala3

def scalajsBinaryVersion = "1"
def scalajsDom = "2.0.0"

def isCI = System.getenv("CI") != null

def isScala2(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2)
def isScala212(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2) && v.exists(_._2 == 12)
def isScala213(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2) && v.exists(_._2 == 13)
def isScala3(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 3)

def jsoniter = List("core", "macros").map { pkg =>
  "com.github.plokhotnyuk.jsoniter-scala" %% s"jsoniter-scala-$pkg" % "2.33.2"
}

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
    val base = (ThisBuild / baseDirectory).value / projectName / "src" / "main"
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
    case partialVersion if isScala213(partialVersion) => if2 ::: if213
    case partialVersion if isScala212(partialVersion) => if2 ::: if212
    case partialVersion if isScala2(partialVersion) => if2
    case partialVersion if isScala3(partialVersion) => if3
    case _ => Nil
  }

inThisBuild(
  List(
    version ~= { dynVer =>
      if (isCI) dynVer
      else {
        import scala.sys.process._
        // drop `v` prefix
        "git describe --abbrev=0 --tags".!!.drop(1).trim + "-SNAPSHOT"
      }
    },
    scalaVersion := scala213,
    crossScalaVersions := allScalaVersions,
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
    resolvers ++= Resolver.sonatypeOssRepos("public"),
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    // faster publishLocal:
    packageDoc / publishArtifact := isCI,
    packageSrc / publishArtifact := isCI,
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
    if2 = List("-Yrangepos", "-deprecation"),
    if213 = List("-release", "11"),
    if212 = List("-Xexperimental", "-release", "8"),
    if3 = List("-language:implicitConversions", "-Ximport-suggestion-timeout", "0")
  )
)

lazy val sharedJavaSettings = List(
  javacOptions ++= Seq("--release", "11")
)

val V = new {
  val scalameta = "4.13.4"

  val munit = "1.1.0"

  val scalacheck = "1.18.1"

  val pprint = "0.9.0"

  val fansi = "0.5.0"

  val fs2 = "3.12.0"

  val metaconfig = "0.15.0"
}

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

lazy val runtime = project
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

lazy val parser = crossProject(JVMPlatform, NativePlatform, JSPlatform)
  .withoutSuffixFor(JVMPlatform)
  .settings(
    sharedSettings,
    moduleName := "mdoc-parser"
  )

lazy val cli = project
  .settings(
    sharedSettings,
    moduleName := "mdoc-cli",
    scalaVersion := scala213,
    crossScalaVersions := allScalaVersions,
    depCoursierInterfaces,
    libraryDependencies ++= List(
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "com.lihaoyi" %% "pprint" % V.pprint,
      "org.scalameta" %% "metaconfig-typesafe-config" % V.metaconfig
    ),
    depScalameta
  )
  .dependsOn(parser.jvm)

lazy val mdoc = project
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
      "io.methvin" % "directory-watcher" % "0.18.0",
      // live reload
      "io.undertow" % "undertow-core" % "2.2.30.Final",
      "org.jboss.xnio" % "xnio-nio" % "3.8.16.Final",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "org.scalameta" %% "metaconfig-typesafe-config" % V.metaconfig,
      "com.lihaoyi" %% "fansi" % V.fansi,
      "com.lihaoyi" %% "pprint" % V.pprint
    )
  )
  .dependsOn(parser.jvm, runtime, cli)
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
    libraryDependencies += depMunit,
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
    depJsDom,
    scalaJSUseMainModuleInitializer := true,
    Compile / npmDependencies ++= List(
      "ms" -> "2.1.1"
    ),
    webpackBundlingMode := BundlingMode.LibraryOnly()
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

val jswebsitedocs = project
  .in(file("tests/websiteJs"))
  .settings(
    sharedSettings,
    publish / skip := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    depJsDom
  )
  .enablePlugins(ScalaJSPlugin)

lazy val worksheets = project
  .in(file("tests/worksheets"))
  .settings(
    sharedSettings,
    publish / skip := true,
    libraryDependencies += depMunit % Test
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
        "io.github.cibotech" %% "evilplot" % "0.9.2"
      )
    ),
    libraryDependencies += depMunit % Test,
    libraryDependencies += {
      val dep = "co.fs2" %% "fs2-core" % V.fs2
      if (isScala3.value) dep.cross(CrossVersion.for3Use2_13) else dep
    },
    buildInfoPackage := "tests.cli",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> (testsInput / Compile / classDirectory).value
    )
  )
  .dependsOn(parser.jvm, mdoc, testsInput, tests)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

lazy val unitJS = project
  .in(file("tests/unit-js"))
  .settings(
    sharedSettings,
    publish / skip := true,
    Compile / unmanagedSourceDirectories ++= multiScalaDirectories("tests/unit-js").value,
    libraryDependencies += depMunit % Test,
    buildInfoPackage := "tests.js",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> (testsInput / Compile / classDirectory).value
    ),
    mdocJS := Some(jsdocs),
    MdocPlugin.mdocJSWorkerClasspath := {
      val _ = (jsWorker / Compile / compile).value

      val folders = Seq(
        (jsWorker / Compile / classDirectory).value
      ) ++ (jsWorker / Compile / resourceDirectories).value

      Some(folders)
    }
  )
  .dependsOn(mdoc, js, testsInput, tests, unit)
  .enablePlugins(BuildInfoPlugin, MdocPlugin)

lazy val plugin = project
  .in(file("mdoc-sbt"))
  .settings(
    sharedSettings,
    sbtPlugin := true,
    scalaVersion := scala212,
    pluginCrossBuild / sbtVersion := "1.1.6",
    crossScalaVersions := List(scala212),
    moduleName := "sbt-mdoc",
    libraryDependencies ++= List(
      "org.jsoup" % "jsoup" % "1.12.1",
      "org.scalacheck" %% "scalacheck" % V.scalacheck % Test,
      depMunit % Test,
      "org.scalameta" %% "testkit" % V.scalameta % Test
    ),
    Compile / resourceGenerators += Def.task {
      val out =
        (Compile / managedResourceDirectories).value.head / "sbt-mdoc.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      props.put("scalaJSVersion", scalaJSVersion)
      IO.write(props, "sbt-mdoc properties", out)
      List(out)
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
  project
    .in(file("mdoc-js-worker"))
    .dependsOn(jsApi)
    .settings(
      sharedSettings,
      moduleName := "mdoc-js-worker",
      libraryDependencies ++= Seq(
        "org.scala-js" %% "scalajs-linker" % scalaJSVersion % Provided cross CrossVersion.for3Use2_13,
        "com.armanbilge" %% "scalajs-importmap" % "0.1.1" cross CrossVersion.for3Use2_13
      )
    )

lazy val js = project
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
    publish / skip := true,
    scalaVersion := scala212,
    crossScalaVersions := List(scala212),
    mdocAutoDependency := false,
    libraryDependencies ++= List(
      "org.scala-sbt" % "sbt" % sbtVersion.value,
      "io.github.cibotech" %% "evilplot" % "0.9.2"
    ),
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    Global / cancelable := true,
    MdocPlugin.autoImport.mdoc := (Compile / run).evaluated,
    mdocJS := Some(jswebsitedocs),
    MdocPlugin.mdocJSWorkerClasspath := {
      val _ = (jsWorker / Compile / compile).value

      val folders = Seq(
        (jsWorker / Compile / classDirectory).value
      ) ++ (jsWorker / Compile / resourceDirectories).value

      Some(folders)
    },
    dependencyOverrides += {
      "org.scala-lang.modules" %%% "scala-xml" % "2.3.0"
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
  .dependsOn(mdoc, js, plugin)
  .enablePlugins(DocusaurusPlugin)

def localCrossPublish(versions: List[String]): Def.Initialize[Task[Unit]] =
  versions
    .map(localCrossPublishProjects)
    .reduceLeft(_ dependsOn _)

def localCrossPublishProjects(scalaV: String): Def.Initialize[Task[Unit]] = {
  val projects = List(parser, runtime, cli, mdoc, js, jsWorker).reverse
  projects
    .flatMap(_.componentProjects)
    .map(p => localCrossPublishProject(p, scalaV))
    .reduceLeft(_ dependsOn _)
}

def localCrossPublishProject(ref: Project, scalaV: String): Def.Initialize[Task[Unit]] =
  Def.task {
    val versionValue = (ThisBuild / version).value
    val projects = List(parser, runtime, cli, mdoc, js, jsWorker)
    val setttings =
      (ThisBuild / version := versionValue) ::
        projects.flatMap(_.componentProjects).map(p => p / scalaVersion := scalaV)
    val newState = Project
      .extract(state.value)
      .appendWithSession(
        setttings,
        state.value
      )
    val _ = Project.extract(newState).runTask(ref / publishLocal, newState)
  }

val depMunit = "org.scalameta" %% "munit" % V.munit

lazy val depJsDom = Def.settings(
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalajsDom
)
