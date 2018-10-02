package mdoc.internal.cli

import mdoc.Reporter
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.MarkdownCompiler

case class Context(settings: Settings, reporter: Reporter, compiler: MarkdownCompiler)

object Context {
  def fromOptions(options: Settings, reporter: Reporter = ConsoleReporter.default): Context = {
    val compiler = MarkdownCompiler.fromClasspath(options.classpath, options.scalacOptions)
    Context(options, reporter, compiler)
  }
}
