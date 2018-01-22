inThisBuild(
  scalaVersion := "2.12.4"
)

lazy val root = project
  .in(file("."))
  .settings(name := "foxRoot")
  .aggregate(fox)

lazy val fox = project
  .settings(
    resolvers += Resolver.bintrayRepo("tpolecat", "maven"),
    libraryDependencies ++= List(
      "io.circe" %% "circe-core" % "0.8.0",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
      "org.tpolecat" %% "tut-core" % "0.5.5",
      "com.lihaoyi" %% "fansi" % "0.2.5",
      "com.lihaoyi" %% "pprint" % "0.5.2",
      "com.lihaoyi" %% "ammonite-ops" % "1.0.3-32-3c3d657",
      ("com.lihaoyi" %% "ammonite-repl" % "1.0.3-32-3c3d657").cross(CrossVersion.full),
      "com.github.alexarchambault" %% "case-app" % "1.2.0-M3",
      "io.circe" %% "circe-config" % "0.3.0",
      "io.circe" %% "circe-generic" % "0.8.0"
    ),
  )
