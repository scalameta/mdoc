import sbt._, Keys._
import sbt.plugins.JvmPlugin

object ScalamdBuild extends AutoPlugin {
  override def requires: Plugins = JvmPlugin
  override def trigger: PluginTrigger = allRequirements
  override def globalSettings = List(
    scalaVersion := "2.12.3"
  )
}
