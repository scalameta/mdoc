package mdoc

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.PathMatcher
import metaconfig.Conf
import metaconfig.Configured
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter

final class MainSettings private (
    private[mdoc] val settings: Settings,
    private[mdoc] val reporter: Reporter
) {

  override def toString: String = {
    s"MainSettings($settings, $reporter)"
  }

  def withArgs(args: List[String]): MainSettings = {
    if (args.isEmpty) this
    else {
      Settings.fromCliArgs(args, settings) match {
        case Configured.Ok(newSettings) =>
          copy(settings = newSettings)
        case Configured.NotOk(error) =>
          throw new IllegalArgumentException(error.toString())
      }
    }
  }
  def withWorkingDirectory(cwd: Path): MainSettings = {
    copy(settings.withWorkingDirectory(AbsolutePath(cwd)))
  }
  def withClasspath(classpath: String): MainSettings = {
    copy(settings.copy(classpath = classpath))
  }
  def withScalacOptions(scalacOptions: String): MainSettings = {
    copy(settings.copy(scalacOptions = scalacOptions))
  }
  def withReporter(reporter: Reporter): MainSettings = {
    copy(reporter = reporter)
  }
  def withCoursierLogger(logger: coursierapi.Logger): MainSettings = {
    copy(settings.copy(coursierLogger = logger))
  }
  def withScreenWidth(screenWidth: Int): MainSettings = {
    copy(settings.copy(screenWidth = screenWidth))
  }
  def withScreenHeight(screenHeight: Int): MainSettings = {
    copy(settings.copy(screenHeight = screenHeight))
  }

  private[this] implicit def cwd: AbsolutePath = settings.cwd
  private[this] def copy(
      settings: Settings = this.settings,
      reporter: Reporter = this.reporter
  ): MainSettings = {
    new MainSettings(settings, reporter)
  }
}

object MainSettings {
  def apply(workingDirectory: Path): MainSettings = {
    val settings = Settings.default(AbsolutePath(workingDirectory))
    val reporter = ConsoleReporter.default
    new MainSettings(settings, reporter)
  }
  def apply(): MainSettings = {
    MainSettings(PathIO.workingDirectory.toNIO)
  }
}
