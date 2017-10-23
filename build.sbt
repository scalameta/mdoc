lazy val root = project
  .copy(id = "foxRoot")
  .in(file("."))
  .settings(
    name := "foxRoot"
  )

lazy val fox = project.settings(
  resolvers += Resolver.bintrayRepo("tpolecat", "maven"),
  libraryDependencies ++= List(
    "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
    "org.tpolecat" %% "tut-core" % "0.5.5",
    "com.lihaoyi" %% "fansi" % "0.2.5",
    "com.lihaoyi" %% "pprint" % "0.5.2",
    "com.github.alexarchambault" %% "case-app" % "1.2.0-M3"
  )
)
