package mdoc

import sbt.Keys._
import sbt._
import scala.collection.mutable.ListBuffer

object MdocPlugin extends AutoPlugin {
  object autoImport {
    val mdocVariables =
      settingKey[Map[String, String]]("Site variables such as @VERSION@.")
    val mdocIn =
      settingKey[File]("Input directory containing markdown sources to be processed by mdoc.")
    val mdocOut =
      settingKey[File]("Output directory for mdoc generated markdown.")
  }
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = List(
    mdocIn := baseDirectory.in(ThisBuild).value / "docs",
    mdocOut := target.in(Compile).value / "mdoc",
    mdocVariables := Map.empty,
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
