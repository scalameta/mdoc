lazy val root = project
  .copy(id = "foxRoot")
  .in(file("."))
  .settings(
    name := "foxRoot"
  )
  .aggregate(fox)

lazy val paiges = project
  .in(file("paiges/core"))
  .settings(
    scalacOptions += "-Yrangepos",
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % "2.0.1" cross CrossVersion.full
    )
  )

lazy val fox = project
  .settings(
    resolvers += Resolver.bintrayRepo("tpolecat", "maven"),
    TaskKey[Unit]("fox") := Def
      .taskDyn[Unit] {
        val cp = fullClasspath
          .in(paiges, Compile)
          .value
          .map(_.data.getAbsolutePath)
          .mkString(java.io.File.pathSeparator)
        val in = resourceDirectory.in(Compile).value / "docs"
        val args = List(
          "",
          "--classpath",
          cp,
          "--in",
          in.getAbsolutePath,
          "--clean-target",
          "--base-url",
          "/target/fox"
        ).mkString(" ")
        println(args)
        run.in(Compile).toTask(args)
      }
      .value,
    // TODO(olafur) validate this is needed
    WebKeys.webJars.in(Assets) := {
      val out = WebKeys.webJars.in(Assets).value
      WebKeys.webJarsDirectory
        .in(Assets)
        .value
        .**(
          "*.min.js" |
            "*.min.css" |
            "lang-*.js" |
            "prettify.css" |
            "prettify.js"
        )
        .get
        .filter(_.isFile)
    },
    (managedClasspath in Runtime) += (packageBin in Assets).value,
    libraryDependencies ++= List(
      "io.circe" %% "circe-core" % "0.8.0",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "org.scalameta" %% "scalameta" % "2.0.1",
      "org.scalameta" %% "metadoc-cli" % "0.1.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "org.tpolecat" %% "tut-core" % "0.5.5",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "com.lihaoyi" %% "pprint" % "0.5.2",
      "com.lihaoyi" %% "ammonite-ops" % "1.0.3",
      "com.github.alexarchambault" %% "case-app" % "1.2.0-M3"
    ),
    libraryDependencies ++= List(
      "org.webjars.npm" % "lunr" % "2.1.0" % Provided,
      "org.webjars" % "prettify" % "4-Mar-2013-1" % Provided,
      "org.webjars" % "modernizr" % "2.8.3" % Provided,
      Seq("animation", "base", "ripple", "rtl", "theme", "typography").foldLeft(
        "org.webjars.npm" % "material__tabs" % "0.3.1" % Provided
      ) { (lib, dep) =>
        lib.exclude("org.webjars.npm", s"material__$dep")
      }
    )
  )
  .dependsOn(paiges) // temporary, only for testing purpuses.
  .enablePlugins(SbtWeb)
