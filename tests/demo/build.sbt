lazy val docs = project
  .in(file("demo-docs"))
  .settings(
    mdocVariables := Map(
      "VERSION" -> "1.0.0"
    )
  )
  .enablePlugins(MdocPlugin)
