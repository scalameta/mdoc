package mdoc.internal.markdown

import coursierapi.Dependency
import coursierapi.Repository

final case class IvyResolution(
    dependencies: Set[Dependency],
    repositories: Set[Repository]
)
