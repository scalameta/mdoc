package mdoc.internal.cli

import scala.collection.JavaConverters._
import mdoc.Reporter
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.internal.markdown.MarkdownBuilder
import coursierapi.Dependency
import scala.collection.mutable
import scala.meta.io.Classpath
import scala.meta.io.AbsolutePath
import mdoc.internal.markdown.Instrumented
import coursierapi.Logger

case class Context(
    settings: Settings,
    reporter: Reporter,
    compiler: MarkdownCompiler,
    compilers: mutable.Map[(Set[Dependency], List[String]), MarkdownCompiler] = mutable.Map.empty
) {
  def compiler(instrumented: Instrumented) = {
    val scalacOptions = instrumented.scalacOptionImports.map(_.value)
    compilers.getOrElseUpdate(
      (instrumented.dependencies, scalacOptions),
      Dependencies.newCompiler(settings, instrumented)
    )
  }
}

object Context {
  def fromCompiler(options: Settings, reporter: Reporter, compiler: MarkdownCompiler): Context = {
    new Context(options, reporter, compiler)
  }
  def fromOptions(options: Settings, reporter: Reporter = ConsoleReporter.default): Context = {
    val compiler = MarkdownBuilder.fromClasspath(options.classpath, options.scalacOptions)
    fromCompiler(options, reporter, compiler)
  }
}
