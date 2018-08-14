package vork.internal.cli

import vork.Logger
import vork.internal.io.ConsoleLogger
import vork.internal.markdown.MarkdownCompiler

case class Context(settings: Settings, logger: Logger, compiler: MarkdownCompiler)

object Context {
  def fromOptions(options: Settings, logger: Logger = ConsoleLogger.default): Context = {
    val compiler = MarkdownCompiler.fromClasspath(options.classpath)
    Context(options, logger, compiler)
  }
}
