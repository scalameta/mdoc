// scalafmt: { maxColumn = 120 }

import sbt.*
import sbt.Keys.*
import sbt.VirtualAxis.PlatformAxis

object Extensions {

  object V {

    val scalameta = "4.17.3"

    val munit = "1.3.6"

    val scalacheck = "1.20.0"

    val pprint = "0.9.6"

    val fansi = "0.5.1"

    val fs2 = "3.13.0"

    val metaconfig = "0.18.8"

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

  def unpublished = Def.settings(publish / skip := true)

  // sbt runs a `;`-separated list, and the leading separator is required
  def tasks(ts: Iterable[String]): String = ts.mkString("; ", "; ", "")

  def onEach(task: String, projects: Iterable[Project]): String = tasks(
    projects.map(p => s"${p.id}/$task")
  )

  def onEach(task: String, v: String, jvm: Iterable[Matrix], js: Iterable[Matrix]): String =
    onEach(task, jvm.map(_.jvm(v)) ++ js.map(_.js(v)))

  def srcWithRoot(root: File, dir: String, cfg: String) = root / dir / "src" / cfg

  // `scala-2.13.18`, then every shorter prefix of it
  def scalaVersionDirs(version: String): List[String] = {
    val res = List.newBuilder[String]
    var end = version.length
    while (end > 0) {
      res += s"scala-${version.substring(0, end)}"
      end = version.lastIndexOf('.', end - 1)
    }
    res.result()
  }

  // the trees a platform reads: its own, and every tree it shares
  private val allPlatformAxes =
    Seq(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native).map(_.value).sorted.toIndexedSeq

  private def platformDirs(platform: String): Seq[String] = {
    val builder = Seq.newBuilder[String]
    builder += "shared"
    builder += platform
    val idx = allPlatformAxes.indexOf(platform)
    if (idx >= 0) {
      allPlatformAxes.take(idx).foreach { x => builder += s"$x-$platform" }
      allPlatformAxes.drop(idx + 1).foreach { x => builder += s"$platform-$x" }
    }
    builder.result()
  }

  private def platformOf(axes: Seq[VirtualAxis]) = axes.collectFirst { case a: VirtualAxis.PlatformAxis => a.value }

  // crossProject's layout, wired by hand: a matrix has one base directory, so a cell reads the
  // trees its own platform names. Absent directories are harmless.
  def roots(base: File, cfg: String) = Def.setting(platformOf(virtualAxes.value).fold(Seq.empty[File]) { platform =>
    val variants = "scala" :: scalaVersionDirs(scalaVersion.value)
    // a matrix base may be relative, and a relative source root resolves against the wrong directory
    val root = IO.resolve((ThisBuild / baseDirectory).value, base)
    val res = Seq.newBuilder[File]
    res += srcWithRoot(root, platform, cfg) / "java"
    for (dir <- platformDirs(platform); src = srcWithRoot(root, dir, cfg); variant <- variants) res += src / variant
    res.result()
  })

  def unmanagedSources(base: File) = Def.settings(
    Compile / unmanagedSourceDirectories ++= roots(base, "main").value,
    Test / unmanagedSourceDirectories ++= roots(base, "test").value
  )

  /* `bspEnabled := false` leaves a row out of the BSP workspace, so an IDE does not import it.
   * IntelliJ can't load multiple versions, though, so force 2.13 if `ide.scala` is absent. */
  private val ideScala = {
    val prop = sys.props.getOrElse("ide.scala", "").trim
    if (prop.nonEmpty) Some(prop)
    else if (sys.props.contains("idea.managed")) Some(scala213) // this looks like IntelliJ
    else None
  }

  // an empty set is no filter, so every platform
  private val idePlatforms = {
    val prop = sys.props.getOrElse("ide.platform", "").trim
    if (prop.isEmpty) Set.empty else prop.split("\\s*,\\s*").toSet
  }

  def ideImportFor(axes: Def.Initialize[Seq[VirtualAxis]]) = bspEnabled := ! {
    autoScalaLibrary.value && ideScala.exists(s => s != scalaBinaryVersion.value && s != scalaVersion.value) ||
    idePlatforms.nonEmpty && !platformOf(axes.value).exists(idePlatforms)
  }

  // a project that is not a matrix has no axes to read, and none of them leaves the JVM
  val ideImportJvm = ideImportFor(Def.setting(Seq(VirtualAxis.jvm)))

  implicit class MatrixExtensions(private val self: Matrix) extends AnyVal {
    // projectMatrix names its rows after the val it is assigned to, so rows are added to the
    // receiver and never built here
    def jvmRows(versions: Iterable[String])(ss: String => Def.SettingsDefinition): Matrix =
      versions.foldLeft(sharedSetup)((m, v) => m.crossJvmRows(v)(Nil, ss(v)))

    def crossJvmRows(sv: String*)(axes: List[VirtualAxis], ss: Def.SettingsDefinition*): Matrix =
      self.jvmPlatform(sv, axes, ss.flatMap(_.settings))

    // one row per published version
    def withJvm(ss: Def.SettingsDefinition*): Matrix = crossJvmRows(allScalaVersions *)(Nil, ss *)
    def crossJvm(ss: Def.SettingsDefinition*): Matrix = sharedSetup.withJvm(ss *)

    def withJs(ss: Def.SettingsDefinition*): Matrix = self.jsPlatform(allScalaVersions, ss.flatMap(_.settings))
    def crossJs(ss: Def.SettingsDefinition*): Matrix = sharedSetup.withJs(ss *)

    def withNative(ss: Def.SettingsDefinition*): Matrix = self.nativePlatform(allScalaVersions, ss.flatMap(_.settings))
    def crossNative(ss: Def.SettingsDefinition*): Matrix = sharedSetup.withNative(ss *)

    // the next Scala is tested, never published
    def jvmScala3Next(ss: Def.SettingsDefinition*): Matrix = crossJvmRows(scala3next)(
      List(VirtualAxis.scalaPartialVersion(scala3next)),
      unpublished ++ ss *
    )

    // a row per published version, and one for the next Scala
    def allJvm(ss: Def.SettingsDefinition*): Matrix = self.crossJvm(ss *).jvmScala3Next(ss *)

    // the same rows, where each one is configured from its own version
    def allJvmRows(ss: String => Def.SettingsDefinition): Matrix =
      jvmScala3Next(ss(scala3next)).jvmRows(allScalaVersions)(ss)

    // the shared tree and each platform's own, as crossProject would read them
    def crossAll: Matrix = self.settings(unmanagedSources(self.base)).allJvm().withJs().withNative()

    private def sharedSetup: Matrix = self.settings(ideImportFor(virtualAxes))
  }

}
