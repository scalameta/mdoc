package mdoc.internal.markdown

import scala.meta.inputs.Position
import coursierapi.Dependency
import scala.meta.Name
import scala.util.matching.Regex
import mdoc.internal.BuildInfo
import mdoc.Reporter

final case class PositionedDependency(pos: Position, dep: Dependency) {
  def syntax: String =
    s"${dep.getModule().getOrganization()}:${dep.getModule().getName()}:${dep.getVersion()}"
}

object PositionedDependency {
  val Full: Regex = "(.+):::(.+):(.+)".r
  val Half: Regex = "(.+)::(.+):(.+)".r
  val Java: Regex = "(.+):(.+):(.+)".r
  def fromName(i: Name.Indeterminate, reporter: Reporter): Option[PositionedDependency] = {
    def create(
        org: String,
        artifact: String,
        version: String
    ): Option[PositionedDependency] =
      Some(PositionedDependency(i.pos, Dependency.of(org, artifact, version)))
    i.value match {
      case Full(org, name, version) =>
        create(org, s"${name}_${BuildInfo.scalaVersion}", version)
      case Half(org, name, version) =>
        create(org, s"${name}_${BuildInfo.scalaBinaryVersion}", version)
      case Java(org, name, version) =>
        create(org, name, version)
      case _ =>
        reporter.error(
          i.pos,
          "invalid dependency. To fix this error, use the format `$ORGANIZATION:$ARTIFACT:$NAME`."
        )
        None
    }
  }
}
