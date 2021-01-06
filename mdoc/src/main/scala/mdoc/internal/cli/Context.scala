package mdoc.internal.cli

import java.nio.file.Files
import scala.collection.JavaConverters._
import metaconfig.ConfError
import metaconfig.Configured
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
import scala.meta.internal.io.PathIO

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
  def fromSettings(settings: Settings, reporter: Reporter): Configured[Context] = {
    // import settings._
    if (settings.in.isEmpty) {
      Configured.error(Feedback.mustBeNonEmpty("in"))
    } else if (settings.out.isEmpty) {
      Configured.error(Feedback.mustBeNonEmpty("out"))
    } else if (settings.in.length != settings.out.length) {
      Configured.error(Feedback.inputDifferentLengthOutput(settings.in, settings.out))
    } else {
      val errors: List[Option[ConfError]] = settings.outputByInput.iterator.map {
        case (input, output) =>
          validateInputOutputPair(settings, input, output)
      }.toList
      errors.flatten match {
        case Nil =>
          val context = Context.fromOptions(settings, reporter)
          settings.onLoad(reporter)
          if (reporter.hasErrors) {
            Configured.error("Failed to load modifiers")
          } else {
            Configured.ok(context)
          }
        case errors =>
          errors.foldLeft(ConfError.empty)(_ combine _).notOk
      }
    }
  }

  private def assumedRegularFile(settings: Settings, absPath: AbsolutePath): Boolean = {
    val extension = PathIO.extension(absPath.toNIO)
    settings.markdownExtensions.toSet.contains(extension)
  }

  private def validateInputOutputPair(
      settings: Settings,
      input: AbsolutePath,
      output: AbsolutePath
  ): Option[ConfError] = {
    if (!Files.exists(input.toNIO)) {
      Some(ConfError.fileDoesNotExist(input.toNIO))
    } else if (input == output) {
      Some(ConfError.message(Feedback.inputEqualOutput(input)))
    } else if (output.toNIO.startsWith(input.toNIO) && !assumedRegularFile(settings, output)) {
      Some(ConfError.message(Feedback.outSubdirectoryOfIn(input.toNIO, output.toNIO)))
    } else if (input.isFile && output.isDirectory) {
      Some(ConfError.message(Feedback.outputCannotBeDirectory(input, output)))
    } else if (input.isDirectory && output.isFile) {
      Some(ConfError.message(Feedback.outputCannotBeRegularFile(input, output)))
    } else {
      None
    }
  }
  def fromCompiler(options: Settings, reporter: Reporter, compiler: MarkdownCompiler): Context = {
    new Context(options, reporter, compiler)
  }
  def fromOptions(options: Settings, reporter: Reporter = ConsoleReporter.default): Context = {
    val compiler = MarkdownBuilder.fromClasspath(options.classpath, options.scalacOptions)
    fromCompiler(options, reporter, compiler)
  }
}
