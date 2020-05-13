package mdoc.internal.cli

import scala.meta.io.Classpath
import coursierapi.Dependency
import mdoc.internal.markdown.MarkdownCompiler
import scala.collection.JavaConverters._
import scala.meta.io.AbsolutePath
import coursierapi.Repository
import mdoc.internal.markdown.Instrumented
import coursierapi.ResolutionParams
import coursierapi.Cache
import coursierapi.Logger
import scala.collection.immutable.Nil

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
        settings.scalacOptions
      case options =>
        s"${settings.scalacOptions} ${options.map(_.value).mkString(" ")}"
    }
    MarkdownCompiler.fromClasspath(classpath.syntax, scalacOptions)
  }
}
