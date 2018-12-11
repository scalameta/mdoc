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
  def withExcludePath(excludePath: List[PathMatcher]): MainSettings = {
    copy(settings.copy(exclude = excludePath))
  }
  def withIncludePath(includePath: List[PathMatcher]): MainSettings = {
    copy(settings.copy(include = includePath))
  }
  def withSiteVariables(variables: Map[String, String]): MainSettings = {
    copy(settings.copy(site = variables))
  }
  def withWorkingDirectory(cwd: Path): MainSettings = {
    copy(settings.withWorkingDirectory(AbsolutePath(cwd)))
  }
  def withOut(out: Path): MainSettings = {
    copy(settings.copy(out = AbsolutePath(out)))
  }
  def withIn(in: Path): MainSettings = {
    copy(settings.copy(in = AbsolutePath(in)))
  }
  def withClasspath(classpath: String): MainSettings = {
    copy(settings.copy(classpath = classpath))
  }
  def withScalacOptions(scalacOptions: String): MainSettings = {
    copy(settings.copy(scalacOptions = scalacOptions))
  }
  def withStringModifiers(modifiers: List[StringModifier]): MainSettings = {
    copy(settings.copy(stringModifiers = modifiers))
  }
  def withCleanTarget(cleanTarget: Boolean): MainSettings = {
    copy(settings.copy(cleanTarget = cleanTarget))
  }
  def withWatch(watch: Boolean): MainSettings = {
    copy(settings.copy(watch = watch))
  }
  @deprecated("Use withCheck instead", "0.4.1")
  def withTest(test: Boolean): MainSettings = {
    copy(settings.copy(check = test))
  }
  def withCheck(check: Boolean): MainSettings = {
    copy(settings.copy(check = check))
  }
  def withReportRelativePaths(reportRelativePaths: Boolean): MainSettings = {
    copy(settings.copy(reportRelativePaths = reportRelativePaths))
  }
  def withCharset(charset: Charset): MainSettings = {
    copy(settings.copy(charset = charset))
  }
  def withReporter(reporter: Reporter): MainSettings = {
    copy(reporter = reporter)
  }
  def withInputStream(inputStream: InputStream): MainSettings = {
    copy(settings.copy(inputStream = inputStream))
  }
  def withHeaderIdGenerator(headerIdGenerator: String => String): MainSettings = {
    copy(settings.copy(headerIdGenerator = headerIdGenerator))
  }
  def withVariablePrinter(variablePrinter: Variable => String): MainSettings = {
    copy(settings.copy(variablePrinter = variablePrinter))
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
