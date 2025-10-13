ThisBuild / scalaVersion := "3.3.7"

enablePlugins(MdocPlugin)

// Add duplicate scalacOptions that are already in Scala 3 defaultFlags
scalacOptions ++= Seq("-deprecation", "-unchecked")

TaskKey[Unit]("check") := {
  val file = mdocOut.value / "readme.md"
  val obtained = IO.read(file)
  IO.delete(file)
  println(s"----${scalaVersion.value}----")
  println(obtained)
  println()
  assert(
    obtained.trim == """
# Test
```scala
val x = 1
// x: Int = 1
```
""".trim,
    "\"\"\"\n" + obtained + "\n\"\"\""
  )
  println("------------------")
}
