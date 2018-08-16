package vork

import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.PathMatcher
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import vork.internal.cli.Settings
import vork.internal.io.ConsoleReporter

final class MainSettings private (
    private[vork] val settings: Settings,
    private[vork] val reporter: Reporter
) {
  def withExcludePath(excludePath: List[PathMatcher]): MainSettings = {
    copy(settings.copy(excludePath = excludePath))
  }
  def withIncludePath(includePath: List[PathMatcher]): MainSettings = {
    copy(settings.copy(includePath = includePath))
  }
  def withSiteVariables(variables: Map[String, String]): MainSettings = {
    copy(settings.copy(site = variables))
  }
  def withWorkingDirectory(cwd: Path): MainSettings = {
    copy(settings.copy(cwd = AbsolutePath(cwd)))
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
  def withStringModifiers(modifiers: List[StringModifier]): MainSettings = {
    copy(settings.copy(stringModifiers = modifiers))
  }
  def withCleanTarget(cleanTarget: Boolean): MainSettings = {
    copy(settings.copy(cleanTarget = cleanTarget))
  }
  def withWatch(watch: Boolean): MainSettings = {
    copy(settings.copy(watch = watch))
  }
  def withTest(test: Boolean): MainSettings = {
    copy(settings.copy(test = test))
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

  private[this] implicit def cwd: AbsolutePath = settings.cwd
  private[this] def copy(
      settings: Settings = this.settings,
      reporter: Reporter = this.reporter
  ): MainSettings = {
    new MainSettings(settings, reporter)
  }
}

object MainSettings {
  def apply(): MainSettings = {
    val settings = Settings.default(PathIO.workingDirectory)
    val reporter = ConsoleReporter.default
    new MainSettings(settings, reporter)
  }
}
