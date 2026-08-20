import sbt._
import sbt.Keys._

object Extensions {

  object V {

    val scalameta = "4.17.3"

    val munit = "1.3.4"

    val scalacheck = "1.19.0"

    val pprint = "0.9.6"

    val fansi = "0.5.1"

    val fs2 = "3.13.0"

    val metaconfig = "0.18.7"

  }

  // sbt 1 took projectMatrix from a plugin; sbt 2 has it built in
  type Matrix = sbt.ProjectMatrix

  def scala212 = "2.12.21"
  def scala213 = "2.13.18"
  def scala3 = "3.3.8"
  def scala3next = "3.8.4"
  def scala2Versions = List(scala212, scala213)
  def allScalaVersions = scala2Versions :+ scala3

  val isScala212 = Def.setting {
    VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("2.12.x"))
  }

  val isScala3 = Def.setting {
    // doesn't work well with >= 3.0.0 for `3.0.0-M1`
    VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("<=1.0.0 || >=2.99.0"))
  }

  // the next Scala is tested, never published
  def nextRow = List(VirtualAxis.scalaPartialVersion(scala3next))
  def unpublished = Def.settings(publish / skip := true)

  // sbt runs a `;`-separated list, and the leading separator is required
  def tasks(ts: Iterable[String]): String = ts.mkString("; ", "; ", "")

  def onEach(task: String, projects: Iterable[Project]): String = tasks(
    projects.map(p => s"${p.id}/$task")
  )

  def onEach(task: String, v: String, jvm: Iterable[Matrix], js: Iterable[Matrix]): String =
    onEach(task, jvm.map(_.jvm(v)) ++ js.map(_.js(v)))

  def srcWithRoot(root: File, dir: String, cfg: String) = root / dir / "src" / cfg

  // crossProject's layout, wired by hand: a matrix has one base directory, so
  // each cell names the trees it shares. Absent directories are harmless.
  def roots(base: File, cfg: String, dirs: String*) = Def.setting[Seq[File]] {
    val variants = List("scala", "java", if (isScala3.value) "scala-3" else "scala-2")
    // a matrix base may be relative, and a relative source root resolves against the wrong directory
    val root = IO.resolve((ThisBuild / baseDirectory).value, base)
    for (dir <- dirs; base = srcWithRoot(root, dir, cfg); variant <- variants)
      yield base / variant
  }

  def unmanagedSources(base: File, dirs: String*) = Def.settings(
    Compile / unmanagedSourceDirectories ++= roots(base, "main", dirs: _*).value,
    Test / unmanagedSourceDirectories ++= roots(base, "test", dirs: _*).value
  )

  implicit class MatrixExtensions(private val self: Matrix) extends AnyVal {
    // projectMatrix names its rows after the val it is assigned to, so rows are added to the
    // receiver and never built here
    def jvmRows(versions: Iterable[String])(ss: String => Def.SettingsDefinition): Matrix =
      versions.foldLeft(self)((m, v) => m.jvmPlatform(Seq(v), Nil, ss(v).settings))

    // a row per published version, and one for the next Scala
    def allJvm(ss: Def.SettingsDefinition*): Matrix = {
      val settings = ss.flatMap(_.settings)
      self.jvmPlatform(allScalaVersions, settings)
        .jvmPlatform(List(scala3next), nextRow, settings ++ unpublished)
    }

    // the same rows, where each one is configured from its own version
    def allJvmRows(ss: String => Def.SettingsDefinition): Matrix = self
      .jvmPlatform(List(scala3next), nextRow, unpublished ++ ss(scala3next).settings)
      .jvmRows(allScalaVersions)(ss)

    // the shared tree and each platform's own, as crossProject would read them
    def crossAll = self.allJvm(unmanagedSources(self.base, "shared", "jvm"))
      .jsPlatform(allScalaVersions, unmanagedSources(self.base, "shared", "js"))
      .nativePlatform(allScalaVersions, unmanagedSources(self.base, "shared", "native"))
  }

}
