lazy val scalamd = project.settings(
  libraryDependencies ++= List(
    "com.vladsch.flexmark" % "flexmark-all" % "0.26.4",
    "com.lihaoyi" %% "fansi" % "0.2.5",
    "com.lihaoyi" %% "pprint" % "0.5.2",
    "com.github.alexarchambault" %% "case-app" % "1.2.0-M3"
  )
)
