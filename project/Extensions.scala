import sbt._
import sbt.Keys._

object Extensions {

  object V {

    val scalameta = "4.17.3"

    val munit = "1.3.4"

    val scalacheck = "1.19.0"

    val pprint = "0.9.6"

    val fansi = "0.5.1"

    val fs2 = "3.13.0"

    val metaconfig = "0.18.7"

  }

  def scala212 = "2.12.21"
  def scala213 = "2.13.18"
  def scala3 = "3.3.8"
  def scala3next = "3.8.4"
  def scala2Versions = List(scala212, scala213)
  def allScalaVersions = scala2Versions :+ scala3

  val isScala212 = Def.setting {
    VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("2.12.x"))
  }

  val isScala3 = Def.setting {
    // doesn't work well with >= 3.0.0 for `3.0.0-M1`
    VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("<=1.0.0 || >=2.99.0"))
  }

  def unpublished = Def.settings(publish / skip := true)

}
