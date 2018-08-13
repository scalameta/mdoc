package vork

import vork.markdown.processors.MarkdownCompiler

case class Context(args: Args, logger: Logger, compiler: MarkdownCompiler)

object Context {
  def fromOptions(options: Args, logger: Logger = Logger.default): Context = {
    val compiler = MarkdownCompiler.fromClasspath(options.classpath)
    Context(options, logger, compiler)
  }
}
