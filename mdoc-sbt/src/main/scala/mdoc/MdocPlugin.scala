package mdoc

import sbt.Keys._
import sbt._
import scala.collection.mutable.ListBuffer

object MdocPlugin extends AutoPlugin {
  object autoImport {
    val mdoc =
      inputKey[Unit]("Run mdoc to generate markdown sources.")
    val mdocCode =
      taskKey[Unit]("Run mdoc and launch VSCode with the mdoc extension installed.")
    val mdocVariables =
      settingKey[Map[String, String]]("Site variables such as @VERSION@.")
    val mdocIn =
      settingKey[File]("Input directory containing markdown sources to be processed by mdoc.")
    val mdocOut =
      settingKey[File]("Output directory for mdoc generated markdown.")
    val mdocAutoDependency =
      settingKey[Boolean]("If true, add mdoc as a library dependency this project.")
  }
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = List(
    mdocIn := baseDirectory.in(ThisBuild).value / "docs",
    mdocOut := target.in(Compile).value / "mdoc",
    mdocVariables := Map.empty,
    mdocAutoDependency := true,
    mdoc := Def.inputTaskDyn {
      val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed
      Def.taskDyn {
        runMain.in(Compile).toTask(s" mdoc.Main ${parsed.mkString(" ")}")
      }
    }.evaluated,
    mdocCode := {
      mdoc.toTask(" ").value
      import sys.process._
      List("code", "--install-extension", "geirsson.mdoc").!!
      val cwd = baseDirectory.in(ThisBuild).value.toString
      List("code", cwd).!!
    },
    libraryDependencies ++= {
      if (mdocAutoDependency.value) {
        List("com.geirsson" %% "mdoc" % BuildInfo.version)
      } else {
        List()
      }
    },
    resourceGenerators.in(Compile) += Def.task {
      val out =
        managedResourceDirectories.in(Compile).value.head / "mdoc.properties"
      val props = new java.util.Properties()
      mdocVariables.value.foreach {
        case (key, value) =>
          props.put(key, value)
      }
      props.put("in", mdocIn.value.toString)
      props.put("out", mdocOut.value.toString)
      props.put(
        "scalacOptions",
        scalacOptions.in(Compile).value.mkString(" ")
      )
      val classpath = ListBuffer.empty[File]
      classpath ++= dependencyClasspath.in(Compile).value.iterator.map(_.data)
      classpath += classDirectory.in(Compile).value
      props.put(
        "classpath",
        classpath.mkString(java.io.File.pathSeparator)
      )
      IO.write(props, "mdoc properties", out)
      List(out)
    }
  )

}
