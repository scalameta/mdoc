package vork.internal.cli

import vork.Reporter
import vork.internal.io.ConsoleReporter
import vork.internal.markdown.MarkdownCompiler

case class Context(settings: Settings, reporter: Reporter, compiler: MarkdownCompiler)

object Context {
  def fromOptions(options: Settings, reporter: Reporter = ConsoleReporter.default): Context = {
    val compiler = MarkdownCompiler.fromClasspath(options.classpath)
    Context(options, reporter, compiler)
  }
}
