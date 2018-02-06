inThisBuild(
  List(
    scalaVersion := "2.12.4",
    organization := "com.geirsson",
    publishTo := Some {
      if (version.value.endsWith("-SNAPSHOT")) Opts.resolver.sonatypeSnapshots
      else Opts.resolver.sonatypeStaging
    },
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
    ),
    version ~= { old =>
      val suffix = if (sys.props.contains("vork.snapshot")) "-SNAPSHOT" else ""
      old.replace('+', '-') + suffix
    }
  )
)

lazy val V = new {
  val pprint = "0.5.2"
}

commands += Command.command("ci-release") { s =>
  "vork/publishSigned" ::
    s
}

lazy val root = project
  .in(file("."))
  .settings(name := "vorkRoot")
  .aggregate(vork)

lazy val runtime = project
  .settings(
    libraryDependencies ++= List(
      scalaOrganization.value % "scala-reflect" % scalaVersion.value,
      "com.lihaoyi" %% "pprint" % V.pprint
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
    cancelable in Global := true,
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalameta" %% "scalameta" % "3.2.0",
      "com.geirsson" %% "metaconfig-core" % "0.6.0",
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.6.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "com.lihaoyi" %% "pprint" % V.pprint,
      "com.lihaoyi" %% "ammonite-ops" % "1.0.3-32-3c3d657",
      "io.methvin" % "directory-watcher" % "0.4.0",
      "ch.epfl.scala" %% "scalafix-core" % "0.5.9",
      ("com.lihaoyi" %% "ammonite-repl" % "1.0.3-32-3c3d657").cross(CrossVersion.full)
    ),
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "org.scalameta" %% "testkit" % "2.1.7" % Test
    ),
    buildInfoPackage := "vork.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      "testsInputClassDirectory" -> classDirectory.in(testsInput, Compile).value
    ),
    compile.in(Test) := compile.in(Test).dependsOn(compile.in(testsInput, Compile)).value
  )
  .dependsOn(runtime)
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = project.in(file("tests/input"))

inScope(Global)(
  Seq(
    credentials ++= (for {
      username <- sys.env.get("SONATYPE_USERNAME")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield
      Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        username,
        password
      )).toSeq,
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
