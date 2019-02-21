package tests.markdown

import mdoc.internal.BuildInfo

object Compat {
  def isScala212: Boolean = BuildInfo.scalaVersion.startsWith("2.12")
  def apply(default: String, compat: Map[String, String]): String = {
    compat
      .get(BuildInfo.scalaVersion)
      .orElse(compat.get(BuildInfo.scalaBinaryVersion))
      .getOrElse(
        BuildInfo.scalaBinaryVersion match {
          case "2.11" =>
            default
              .replaceAllLiterally("Predef.scala:288", "Predef.scala:230")
          case _ => default
        }
      )
  }
}
