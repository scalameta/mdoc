package mdoc.internal.cli
import metaconfig.Configured
import metaconfig.generic.Surface
import metaconfig.ConfEncoder
import metaconfig.ConfDecoder
trait SettingsGeneric {
  // note(@tgodzik) not used currently for Scala 3
  def fromCliArgs(args: List[String], base: Settings): Configured[Settings] = Configured.ok(base)
  def write(settings: Settings) = throw NotImplementedError(
    "Implement ConfEncoder derivation for Settings in Scala3"
  )

  implicit def surf: Surface[Settings] =
    throw NotImplementedError("Implement Surface derivation for Settings in Scala3")

  implicit def cd: ConfDecoder[Settings] =
    throw NotImplementedError("Implement ConfDecoder derivation for Settings in Scala3")

  implicit def ce: ConfEncoder[Settings] =
    throw NotImplementedError("Implement ConfEncoder derivation for Settings in Scala3")
}
