# Contributing

## Narrowing what an IDE imports

sbt builds each project of this build once per Scala version and platform. Several of those rows
use the same source directories. Two system properties control which rows an IDE imports: the
build sets `bspEnabled := false` on the other rows, and sbt then leaves them out of the BSP
workspace.

- `-Dide.scala=X` — sbt keeps only the rows for Scala version `X`.
  - matches full or binary version
  - if unspecified or empty: keep all scala versions
    - IntelliJ only: will be forced to `2.13`; see below why IntelliJ can't load multiple versions.
- `-Dide.platform=Y` — sbt keeps only the rows for the platforms in `Y`, a comma-separated list.
  - matches `jvm`, `js`, or `native`
  - if unspecified: keep every platform
  - `-Dide.platform=`, with nothing after it, keeps every platform

IntelliJ cannot import the whole matrix. It puts the sources that several rows use into one module,
and then compiles the Scala 2 and the Scala 3 sources of a project together. It starts sbt with
`-Didea.managed=true`. If you do not set `-Dide.scala`, that property selects 2.13. To choose
another version, add `-Dide.scala=X` under `Settings -> Build, Execution, Deployment -> Build Tools
-> sbt -> VM parameters`, then reload the sbt project.

These properties change what an IDE imports over BSP. A command-line `sbt` is not a BSP client, so
it still sees every row and builds and tests them all.

An sbt server runs with the system properties from its own command line. A later `sbt` in the same
directory attaches to that server, so a property you pass then changes nothing. Run `sbt shutdown`
before you test a change to these properties.
