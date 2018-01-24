package fox
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.generic
import metaconfig.generic.Surface
import metaconfig.typesafeconfig._
import java.nio.file.Path

case class Config(site: Map[String, String] = Map.empty)
object Config {
  val default = Config()
  implicit val surface: Surface[Config] = generic.deriveSurface[Config]
  implicit val decoder: ConfDecoder[Config] = generic.deriveDecoder[Config](default)
  def fromPath(path: Path): Config = {
    val conf = Conf.parseFile(path.toFile).get
    ConfDecoder[Config].read(conf).get
  }
}
