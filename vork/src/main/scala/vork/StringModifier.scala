package vork

import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.generic.Surface
import scala.meta.inputs.Input
import vork.internal.cli.Settings

trait StringModifier {
  val name: String
  def process(info: String, code: Input, reporter: Reporter): String
}

object StringModifier {
  implicit val surface: Surface[Settings] = new Surface(Nil)
  implicit val decoder: ConfDecoder[StringModifier] =
    ConfDecoder.instanceF[StringModifier](_ => ConfError.message("unsupported").notOk)
  implicit val encoder: ConfEncoder[StringModifier] =
    ConfEncoder.StringEncoder.contramap(mod => s"<${mod.name}>")
}
