package mdoc.internal.cli

import scala.collection.JavaConverters._
import scala.meta.io.AbsolutePath

case class MdocProperties(
    scalacOptions: String = "",
    classpath: String = "",
    site: Map[String, String] = Map.empty,
    in: Option[AbsolutePath] = None,
    out: Option[AbsolutePath] = None
)

object MdocProperties {
  def default(cwd: AbsolutePath): MdocProperties = {
    val path = "mdoc.properties"
    Option(this.getClass.getClassLoader.getResourceAsStream(path)) match {
      case Some(resource) =>
        val props = new java.util.Properties()
        try props.load(resource)
        finally resource.close()
        def getPath(key: String): Option[AbsolutePath] =
          Option(props.getProperty(key)).map(AbsolutePath(_)(cwd))
        MdocProperties(
          scalacOptions = props.getProperty("scalacOptions", ""),
          classpath = props.getProperty("classpath", ""),
          site = props.asScala.toMap,
          in = getPath("in"),
          out = getPath("out")
        )
      case None =>
        MdocProperties()
    }
  }
}
