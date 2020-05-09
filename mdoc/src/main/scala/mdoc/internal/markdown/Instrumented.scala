package mdoc.internal.markdown

import coursierapi.Dependency
import mdoc.internal.BuildInfo
import scala.util.matching.Regex
import scala.meta.Name
import scala.collection.mutable
import coursierapi.IvyRepository
import coursierapi.Repository
import coursierapi.MavenRepository
import mdoc.Reporter
import scala.util.Failure
import scala.util.Success
import coursierapi.error.CoursierError

case class Instrumented(
    source: String,
    scalacOptionImports: List[Name.Indeterminate],
    dependencyImports: List[Name.Indeterminate],
    repositoryImports: List[Name.Indeterminate],
    positionedDependencies: List[PositionedDependency],
    dependencies: Set[Dependency],
    repositories: List[Repository]
)

object Instrumented {
  def fromSource(
      source: String,
      scalacOptionImports: List[Name.Indeterminate],
      dependencyImports: List[Name.Indeterminate],
      repositoryImports: List[Name.Indeterminate],
      reporter: Reporter
  ): Instrumented = {
    val positioned = dependencyImports.flatMap(i => PositionedDependency.fromName(i, reporter))
    val dependencies = positioned.map(_.dep).toSet
    val repositories =
      for {
        name <- repositoryImports
        repo <- SharedRepositoryParser.repository(name.value) match {
          case Failure(exception) =>
            exception match {
              case _: CoursierError =>
                reporter.error(name.pos, exception.getMessage())
              case _ =>
                reporter.error(name.pos, exception)
            }
            Nil
          case Success(value) =>
            List(value)
        }
      } yield repo
    Instrumented(
      source,
      scalacOptionImports,
      dependencyImports,
      repositoryImports,
      positioned,
      dependencies,
      repositories
    )
  }
}
