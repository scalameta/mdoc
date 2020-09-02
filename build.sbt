import scala.collection.mutable

def scala212 = "2.12.11"
def scala211 = "2.11.12"
def scala213 = "2.13.2"
def scala3 = "0.26.0-RC1"

def scalajs = "1.1.1"
def scalajsBinaryVersion = "1"
def scalajsDom = "1.0.0"

def isScala2(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2)
def isScala212(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2) && v.exists(_._2 == 12)
def isScala211(v: Option[(Long, Long)]): Boolean = v.exists(_._1 == 2) && v.exists(_._2 == 11)
def isScala3(v: Option[(Long, Long)]): Boolean =
  v.exists(_._1 == 0) || v.exists(_._1 == 3)

val isScala213 = Def.setting {
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13"))
}

val isScala3 = Def.setting {
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("<=1.0.0 || >=3.0.0"))
}

val isScalaJs1 = Def.setting {
  VersionNumber(scalaJSVersion).matchesSemVer(SemanticSelector(">=1.0.0"))
}

def multiScalaDirectories(projectName: String) =
  Def.setting {
    val root = baseDirectory.in(ThisBuild).value / projectName
    val base = root / "src" / "main"
    val result = mutable.ListBuffer.empty[File]
    val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
    partialVersion.collect {
      case (major, minor) =>
        result += base / s"scala-$major.$minor"
    }
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
    scalaVersion := scala212,
    crossScalaVersions := List(scala212, scala211, scala213, scala3),
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
  scalacOptions ++= crossSetting(
    scalaVersion.value,
    if2 = List("-target:jvm-1.8", "-Yrangepos", "-deprecation"),
    if212 = List("-Xexperimental"),
    if211 = List("-Xexperimental"),
    if3 = List("-language:implicitConversions", "-Ximport-suggestion-timeout", "0")
  )
)

val V = new {
  val scalameta = "4.3.20"
  val munit = "0.7.11"
  val coursier = "0.0.24"
}

lazy val pprintVersion = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "0.5.4"
  else "0.6.0"
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
    libraryDependencies += "com.lihaoyi" %% "pprint" % pprintVersion.value,
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if2 = List(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
      )
    )
  )
  .dependsOn(interfaces)

val excludePprint = ExclusionRule(organization = "com.lihaoyi")

lazy val mdoc = project
  .settings(
    sharedSettings,
    unmanagedSourceDirectories.in(Compile) ++= multiScalaDirectories("mdoc").value,
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
    libraryDependencies ++= crossSetting(
      scalaVersion.value,
      if3 = List(
        "ch.epfl.lamp" %% "dotty-compiler" % scalaVersion.value,
        ("org.scalameta" %% "scalameta" % V.scalameta)
          .excludeAll(excludePprint)
          .withDottyCompat(scalaVersion.value),
        ("com.geirsson" %% "metaconfig-typesafe-config" % "0.9.10")
          .excludeAll(excludePprint)
          .withDottyCompat(scalaVersion.value)
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
      "io.get-coursier" % "interface" % V.coursier,
      "com.vladsch.flexmark" % "flexmark-all" % "0.62.2",
      "io.methvin" % "directory-watcher" % "0.10.0",
      // live reload
      "io.undertow" % "undertow-core" % "2.1.3.Final",
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

def scala212LibraryDependencies(deps: List[ModuleID]) =
  List(
    libraryDependencies ++= {
      if (isScala213.value || isScala3.value) Nil
      else deps
    }
  )
val tests = project
  .in(file("tests/tests"))
  .settings(
    sharedSettings,
    skip in publish := true,
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
    skip in publish := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    crossScalaVersions -= scala3,
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

lazy val worksheets = project
  .in(file("tests/worksheets"))
  .settings(
    sharedSettings,
    skip in publish := true,
    libraryDependencies ++= List(
      "org.scalameta" %% "munit" % V.munit % Test
    )
  )
  .dependsOn(mdoc, tests)

lazy val unit = project
  .in(file("tests/unit"))
  .settings(
    sharedSettings,
    skip in publish := true,
    crossScalaVersions -= scala3,
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
    buildInfoPackage := "tests.cli",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> classDirectory.in(testsInput, Compile).value
    ),
    mdocJS := Some(jsdocs)
  )
  .dependsOn(mdoc, js, testsInput, tests)
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
    crossScalaVersions -= scala3,
    moduleName := "mdoc-js",
    scala212LibraryDependencies(
      List(
        "org.scala-js" % "scalajs-compiler" % scalajs cross CrossVersion.full,
        "org.scala-js" %% "scalajs-linker" % scalajs
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
