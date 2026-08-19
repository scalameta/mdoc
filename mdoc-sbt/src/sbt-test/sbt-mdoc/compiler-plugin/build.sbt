scalaVersion := "3.3.8"
lazy val acyclic = ("com.lihaoyi" %% "acyclic" % "0.3.19").cross(CrossVersion.full)
addCompilerPlugin(acyclic)
libraryDependencies += acyclic % "provided"
scalacOptions += "-P:acyclic:force"
enablePlugins(MdocPlugin)
