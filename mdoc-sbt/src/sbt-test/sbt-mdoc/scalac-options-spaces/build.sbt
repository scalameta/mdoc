ThisBuild / scalaVersion := "3.3.7"

enablePlugins(MdocPlugin)

// -Wconf option containing a space in the msg regex pattern.
// Previously this caused mdoc to break the option when re-splitting
// the scalacOptions string by whitespace.
scalacOptions += "-Wconf:msg=unused import:s"

TaskKey[Unit]("check") := {
  val file = mdocOut.value / "readme.md"
  val obtained = IO.read(file)
  IO.delete(file)
  println(s"----${scalaVersion.value}----")
  println(obtained)
  println()
  assert(
    obtained.trim ==
      """
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
