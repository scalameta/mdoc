package fox

case class SiteVariables(variables: Map[String, String])
object SiteVariables {
  def empty: SiteVariables =
    SiteVariables(Map.empty[String, String])
}

case class Config(variables: SiteVariables)

object Config {
  import java.nio.file.Path
  import com.typesafe.config.ConfigFactory
  def from(path: Path): Config = {
    import io.circe.config.syntax._
    val readConfig = ConfigFactory.parseFile(path.toFile)
    val siteVariables = readConfig.as[Map[String, String]]("site.variables") match {
      case Right(success) => SiteVariables(success)
      case l: Left[_, _] => SiteVariables.empty
    }
    Config(siteVariables)
  }
}
