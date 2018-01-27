inThisBuild(
  List(
    scalaVersion := "2.12.4",
    version ~= { old =>
      val suffix = if (sys.props.contains("vork.snapshot")) "-SNAPSHOT" else ""
      old.replace('+', '-') + suffix
    }
  )
)

commands += Command.command("ci-release") { s =>
  "vork/publishSigned" ::
    s
}

lazy val root = project
  .in(file("."))
  .settings(name := "vorkRoot")
  .aggregate(vork)

lazy val vork = project
  .settings(
    fork in run := true,
    cancelable in Global := true,
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.geirsson" %% "metaconfig-core" % "0.6.0",
      "com.geirsson" %% "metaconfig-typesafe-config" % "0.6.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "com.lihaoyi" %% "pprint" % "0.5.2",
      "com.lihaoyi" %% "ammonite-ops" % "1.0.3-32-3c3d657",
      "io.methvin" % "directory-watcher" % "0.4.0",
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
  .enablePlugins(BuildInfoPlugin)

lazy val testsInput = project.in(file("tests/input"))

inScope(Global)(
  Seq(
    publishTo := Some {
      if (version.value.endsWith("-SNAPSHOT")) Opts.resolver.sonatypeSnapshots
      else Opts.resolver.sonatypeStaging
    },
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
