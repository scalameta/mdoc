package tests.markdown

import tests.BuildInfo

object Compat {
  def isScala212: Boolean = BuildInfo.scalaVersion.startsWith("2.12")
  def postProcess(default: String, compat: Map[String, String => String]): String = {
    val processor = compat
      .get(BuildInfo.scalaVersion)
      .orElse(compat.get(BuildInfo.scalaBinaryVersion))
      .orElse(compat.get("all"))
    processor match {
      case None => default
      case Some(fn) => fn(default)
    }
  }
  def apply(
      default: String,
      compat: Map[String, String],
      postProcess: Map[String, String => String] = Map.empty
  ): String = {
    val result = compat
      .collect { case (key, value) if BuildInfo.scalaVersion.startsWith(key) => value }
      .headOption
      .orElse(compat.get("all"))
      .getOrElse(
        BuildInfo.scalaBinaryVersion match {
          case "2.11" =>
            default
              .replace("Predef.scala:288", "Predef.scala:230")
          case "2.12" =>
            default
              .replace("package.scala:219", "package.scala:220")
          case "2.13" =>
            default
              .replace("<init>", "<clinit>")
              .replace("Predef.scala:288", "Predef.scala:347")
          case other if other.startsWith("3.0") =>
            default
              .replace("<init>", "<clinit>")
              .replace("Predef.scala:288", "Predef.scala:345")
          case _ =>
            default
        }
      )
    this.postProcess(result, postProcess)
  }
}
