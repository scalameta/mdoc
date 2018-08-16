package vork.website

import java.nio.file.Paths
import scala.meta.internal.io.PathIO
import vork.Main
import vork.MainSettings
import vork.internal.BuildInfo

object Website {
  def main(args: Array[String]): Unit = {
    val settings = MainSettings()
      .withIn(Paths.get("docs"))
      .withOut(Paths.get("out"))
      .withSiteVariables(Map("VERSION" -> BuildInfo.version))
      .withWatch(true)
      .withCleanTarget(false)
    val exitCode = Main.process(settings)
    sys.exit(exitCode)
  }
}
