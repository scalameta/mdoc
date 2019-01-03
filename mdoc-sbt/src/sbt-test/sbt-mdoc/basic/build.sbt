scalaVersion.in(ThisBuild) := "2.12.8"

enablePlugins(MdocPlugin)
mdocJS := Some(jsapp)

TaskKey[Unit]("check") := {
  val obtained = IO.read(mdocOut.value / "readme.md")
  println()
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

<div id="mdoc-js-run0" data-mdoc-js></div>

<script type="text/javascript" src="readme.md.js" defer></script>

<script type="text/javascript" src="mdoc.js" defer></script>
""".trim,
    "\"\"\"\n" + obtained + "\n\"\"\""
  )
}

lazy val jsapp = project
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6"
  )
  .enablePlugins(ScalaJSPlugin)
