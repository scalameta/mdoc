package mdoc.internal.cli
import metaconfig.Configured
trait SettingsGeneric {
  // note(@tgodzik) not used currently for Scala 3
  def fromCliArgs(args: List[String], base: Settings): Configured[Settings] = Configured.ok(base)
  def write(settings: Settings) = throw NotImplementedError("Implement ConfEncoder derivation for Settings in Scala3")


  def help(displayVersion: String, width: Int): String = 
    throw NotImplementedError("Implement Surface derivation for Settings in Scala3")
}
