import scala.concurrent.duration._
import MdocPlugin._

ThisBuild / scalaVersion := "2.12.17"

enablePlugins(MdocPlugin)

InputKey[Unit]("mdocBg") := Def.inputTaskDyn {
  validateSettings.value
  val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed
  val args = (mdocExtraArguments.value ++ parsed).mkString(" ")
  (Compile / bgRunMain).toTask(s" mdoc.SbtMain $args")
}.evaluated

TaskKey[Unit]("check") := {
  SbtTest.test(
    TestCommand(
      "mdocBg --watch --background",
      "Waiting for file changes (press enter to interrupt)"
    ),
    TestCommand("show version", "[info] 0.1.0-SNAPSHOT", 3.seconds),
    TestCommand("exit")
  )
}
