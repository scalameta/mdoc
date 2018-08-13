package vork.internal.cli

import vork.internal.io.Logger
import vork.internal.markdown.MarkdownCompiler

case class Context(settings: Settings, logger: Logger, compiler: MarkdownCompiler)

object Context {
  def fromOptions(options: Settings, logger: Logger = Logger.default): Context = {
    val compiler = MarkdownCompiler.fromClasspath(options.classpath)
    Context(options, logger, compiler)
  }
}
