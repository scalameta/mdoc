package vork

import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.generic.Surface
import scala.meta.inputs.Input
import vork.internal.cli.Settings

trait CustomModifier {
  val name: String
  def process(info: String, code: Input, reporter: Reporter): String
}

object CustomModifier {
  implicit val surface: Surface[Settings] = new Surface(Nil)
  implicit val decoder: ConfDecoder[CustomModifier] =
    ConfDecoder.instanceF[CustomModifier](_ => ConfError.message("unsupported").notOk)
  implicit val encoder: ConfEncoder[CustomModifier] =
    ConfEncoder.StringEncoder.contramap(mod => s"<${mod.name}>")
}
