ThisBuild / scalaVersion := "2.12.18"
ThisBuild / crossScalaVersions := List("2.12.19", "2.13.13", "3.3.3")

enablePlugins(MdocPlugin)
mdocJS := Some(jsapp)

TaskKey[Unit]("check") := {
  val file = mdocOut.value / "readme.md"
  val obtained = IO.read(file)
  IO.delete(file)
  println(s"----${scalaVersion.value}----")
  println(obtained)
  println()
  assert(
    obtained.trim == """
```scala
println(example.Example.greeting)
// Hello world!
```

```scala
println("Hello Scala.js!")
```
<div id="mdoc-html-run0" data-mdoc-js></div>
<script type="text/javascript" src="readme.md.js" defer></script>
<script type="text/javascript" src="mdoc.js" defer></script>
""".trim,
    "\"\"\"\n" + obtained + "\n\"\"\""
  )
  println("------------------")
}

lazy val jsapp = project
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.0.0"
  )
  .enablePlugins(ScalaJSPlugin)
