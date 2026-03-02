package mdoc.internal.cli

import scala.meta.io.Classpath
import coursierapi.Dependency
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.internal.markdown.MarkdownBuilder
import mdoc.internal.cli.ScalacOptions
import scala.meta.io.AbsolutePath
import coursierapi.Repository
import mdoc.internal.markdown.Instrumented
import coursierapi.ResolutionParams
import coursierapi.Cache
import coursierapi.Logger
import scala.collection.immutable.Nil
import mdoc.internal.pos.PositionSyntax._

object Dependencies {
  def newCompiler(
      settings: Settings,
      instrumented: Instrumented
  ): MarkdownCompiler = {
    val jars = coursierapi.Fetch
      .create()
      .addDependencies(instrumented.dependencies.toArray: _*)
      .addRepositories(instrumented.repositories.toArray: _*)
      .withCache(Cache.create().withLogger(settings.coursierLogger))
      .fetch()
      .asScala
      .map(_.toPath())
    val classpath =
      Classpath(Classpath(settings.classpath).entries ++ jars.map(AbsolutePath(_)))
    val scalacOptions = instrumented.scalacOptionImports match {
      case Nil =>
        ScalacOptions.parse(settings.scalacOptions)
      case options =>
        ScalacOptions.parse(settings.scalacOptions) ++ options.map(_.value)
    }
    MarkdownBuilder.fromClasspath(classpath.syntax, scalacOptions)
  }
}
