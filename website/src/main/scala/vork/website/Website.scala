package vork.website

import java.nio.file.Paths
import scala.meta.internal.io.PathIO
import vork.Main
import vork.MainSettings

object Website {
  def main(args: Array[String]): Unit = {
    val cwd = PathIO.workingDirectory.toNIO
    val settings = MainSettings()
      .withIn(Paths.get("docs"))
      .withOut(Paths.get("out"))
      .withWatch(true)
      .withCleanTarget(false)
    Main.process(settings)
  }
}
