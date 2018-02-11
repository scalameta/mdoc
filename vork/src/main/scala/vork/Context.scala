package vork

import vork.markdown.processors.MarkdownCompiler

case class Context(options: Options, logger: Logger, compiler: MarkdownCompiler)

object Context {
  def fromOptions(options: Options, logger: Logger = Logger.default): Context = {
    val compiler = MarkdownCompiler.fromClasspath(options.classpath)
    Context(options, logger, compiler)
  }
}
