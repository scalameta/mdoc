ThisBuild / scalaVersion := "2.12.17"

enablePlugins(MdocPlugin)

TaskKey[Unit]("check") := {
  SbtTest.test(
    TestCommand("mdocBgStart", "Waiting for file changes (press enter to interrupt)"),
    TestCommand("show version", "[info] 0.1.0-SNAPSHOT"),
    TestCommand("mdocBgStart", "mdoc is already running in the background"),
    TestCommand("mdocBgStop", "stopping mdoc"),
    TestCommand("mdocBgStop", "mdoc is not running in the background"),
    TestCommand("exit")
  )
}
