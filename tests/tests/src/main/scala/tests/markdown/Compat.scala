package tests.markdown

import tests.BuildInfo

object Compat {
  sealed trait ScalaVersion
  case object Scala2 extends ScalaVersion
  case object Scala211 extends ScalaVersion
  case object Scala212 extends ScalaVersion
  case object Scala213 extends ScalaVersion
  case object Scala3 extends ScalaVersion
  case class Full(ver: String) extends ScalaVersion
  case object All extends ScalaVersion

  private def isCurrent(ver: ScalaVersion): Boolean = {
    val full = BuildInfo.scalaVersion
    val binary = BuildInfo.scalaBinaryVersion

    ver match {
      case Scala2 => binary.startsWith("2.")
      case Scala211 => full.startsWith("2.11")
      case Scala212 => full.startsWith("2.12")
      case Scala213 => full.startsWith("2.13")
      case Scala3 => full.startsWith("3.0") || binary == "3"
      case Full(v) => v == full
      case All => true
    }
  }

  private def binary: ScalaVersion = {
    BuildInfo.scalaBinaryVersion match {
      case "2.11" => Scala211
      case "2.12" => Scala212
      case "2.13" => Scala213
      case "3" => Scala3
      case s if s.startsWith("3.0") => Scala3
    }
  }

  def isScala212: Boolean = BuildInfo.scalaVersion.startsWith("2.12")
  def isScala3: Boolean =
    BuildInfo.scalaVersion.startsWith("3.0") || BuildInfo.scalaBinaryVersion == "3"

  def postProcess(default: String, compat: Map[ScalaVersion, String => String]): String = {
    val processor = compat
      .get(Full(BuildInfo.scalaVersion))
      .orElse(compat.get(binary))
      .orElse(compat.get(All))
    processor match {
      case None => default
      case Some(fn) => fn(default)
    }
  }

  def apply(
      default: String,
      compat: Map[ScalaVersion, String],
      postProcess: Map[ScalaVersion, String => String] = Map.empty
  ): String = {
    val result = compat
      .collect { case (key, value) if isCurrent(key) => value }
      .headOption
      .orElse(compat.get(All))
      .getOrElse(
        if (isCurrent(Scala211))
          default
            .replace("Predef.scala:288", "Predef.scala:230")
        else if (isCurrent(Scala212))
          default
            .replace("package.scala:219", "package.scala:220")
        else if (isCurrent(Scala213))
          default
            .replace("<init>", "<clinit>")
            .replace("Predef.scala:288", "Predef.scala:347")
        else if (isCurrent(Scala3))
          default
            .replace("<init>", "<clinit>")
            .replace("Predef.scala:288", "Predef.scala:345")
        else
          default
      )
    this.postProcess(result, postProcess)
  }
}
