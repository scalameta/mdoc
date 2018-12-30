package mdoc

import java.util.Properties

object BuildInfo {
  def version: String =
    props.getProperty("version", "0.8.0-SNAPSHOT")

  private lazy val props: Properties = {
    val props = new Properties()
    val path = "sbt-mdoc.properties"
    val classloader = this.getClass.getClassLoader
    Option(classloader.getResourceAsStream(path)) match {
      case Some(stream) =>
        props.load(stream)
      case None =>
        println(s"error: failed to load $path")
    }
    props
  }
}
