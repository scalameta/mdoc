package mdoc.internal.cli

case class MdocProperties(
    scalacOptions: String = ""
)

object MdocProperties {
  def default(): MdocProperties = {
    val path = "mdoc.properties"
    Option(this.getClass.getClassLoader.getResourceAsStream(path)) match {
      case Some(resource) =>
        val props = new java.util.Properties()
        try props.load(resource)
        finally resource.close()
        MdocProperties(
          scalacOptions = props.getProperty("scalacOptions", "")
        )
      case None =>
        MdocProperties()
    }
  }
}
