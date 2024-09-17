package mdoc.internal.cli

import java.nio.file.Files
import java.util.Properties
import scala.collection.JavaConverters._
import scala.meta.io.AbsolutePath
import java.io.File

case class MdocProperties(
    scalacOptions: String = "",
    classpath: String = "",
    site: Map[String, String] = Map.empty,
    in: Option[List[AbsolutePath]] = None,
    out: Option[List[AbsolutePath]] = None
)

object MdocProperties {
  def fromFile(path: AbsolutePath): MdocProperties = {
    val props = new Properties()
    val in = Files.newInputStream(path.toNIO)
    try props.load(in)
    finally in.close()
    fromProps(props, path)
  }
  def fromProps(props: Properties, cwd: AbsolutePath): MdocProperties = {
    def getPath(key: String): Option[List[AbsolutePath]] = {
      Option(props.getProperty(key)).map { paths =>
        paths.split(File.pathSeparatorChar).toList.map(path => AbsolutePath(path)(cwd))
      }
    }
    MdocProperties(
      scalacOptions = props.getProperty("scalacOptions", ""),
      classpath = props.getProperty("classpath", ""),
      site = props.asScala.toMap,
      in = getPath("in"),
      out = getPath("out")
    )
  }
  def default(cwd: AbsolutePath, path: String): MdocProperties = {
    Option(this.getClass.getClassLoader.getResourceAsStream(path)) match {
      case Some(resource) =>
        val props = new java.util.Properties()
        try props.load(resource)
        finally resource.close()
        fromProps(props, cwd)
      case None =>
        MdocProperties()
    }
  }
}
