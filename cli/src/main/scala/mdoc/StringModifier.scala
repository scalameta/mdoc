package mdoc

import java.util.ServiceLoader
import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.generic.Surface
import scala.meta.inputs.Input
import mdoc.internal.cli.Settings
import scala.collection.JavaConverters._

trait StringModifier {
  val name: String
  def process(info: String, code: Input, reporter: Reporter): String
  override def toString: String = s"StringModifier(mdoc:$name)"
}

object StringModifier {
  def default(): List[StringModifier] = default(this.getClass.getClassLoader)
  def default(classLoader: ClassLoader): List[StringModifier] =
    ServiceLoader.load(classOf[StringModifier], classLoader).iterator().asScala.toList
  implicit val surface: Surface[Settings] = new Surface(Nil)
  implicit val decoder: ConfDecoder[StringModifier] =
    ConfDecoder.from[StringModifier](_ => ConfError.message("unsupported").notOk)
  implicit val encoder: ConfEncoder[StringModifier] =
    ConfEncoder.StringEncoder.contramap(mod => s"<${mod.name}>")
}
