scalaVersion := "2.12.8"

enablePlugins(MdocPlugin)

TaskKey[Unit]("check") := {
  val obtained = IO.read(mdocOut.value / "readme.md")
  assert(obtained.trim == """
```scala
println(example.Example.greeting)
// Hello world!
```
""".trim)
}
