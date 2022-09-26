package mdoc

import java.io.File
import sbt.Keys._
import sbt._
import scala.collection.mutable.ListBuffer

object MdocPlugin extends AutoPlugin {
  object autoImport {
    val mdoc =
      inputKey[Unit](
        "Run mdoc to generate markdown sources. " +
          "Supports arguments like --watch to start the file watcher with livereload."
      )
    val mdocVariables =
      settingKey[Map[String, String]](
        "Site variables that can be referenced from markdown with @VERSION@."
      )
    val mdocIn =
      settingKey[File](
        "Input directory or source file containing markdown to be processed by mdoc. " +
          "Defaults to the toplevel docs/ directory."
      )
    val mdocOut =
      settingKey[File](
        "Output directory or output file name for mdoc generated markdown. " +
          "Defaults to the target/mdoc directory of this project. " +
          "If this is a file name, it assumes your `in` was also an individual file"
      )
    val mdocExtraArguments =
      settingKey[Seq[String]](
        "Additional command-line arguments to pass on every mdoc invocation. " +
          "For example, add '--no-link-hygiene' to disable link hygiene."
      )
    val mdocJS =
      settingKey[Option[Project]](
        "Optional Scala.js classpath and compiler options to use for the mdoc:js modifier. " +
          "To use this setting, set the value to `mdocJS := Some(jsproject)` where `jsproject` must be a Scala.js project."
      )
    val mdocJSLibraries =
      taskKey[Seq[Attributed[File]]](
        "Additional local JavaScript files to load before loading the mdoc compiled Scala.js bundle. " +
          "If using scalajs-bundler, set this key to `webpack.in(<mdocJS project>, Compile, fullOptJS).value`."
      )
    val mdocAutoDependency =
      settingKey[Boolean](
        "If false, do not add mdoc as a library dependency this project. " +
          "Default value is true."
      )
  }
  val mdocInternalVariables =
    settingKey[List[(String, String)]](
      "Additional site variables that are added by mdoc plugins. Not intended for public use."
    )
  import autoImport._

  lazy val validateSettings = Def.task[Unit] {
    val in = mdocIn.value.toPath
    val base = baseDirectory.value.toPath
    if (in == base) {
      throw MdocException(
        s"mdocIn and baseDirectory cannot have the same value '$in'. " +
          s"To fix this problem, either customize the project baseDirectory with `in(file('myproject-docs'))` or " +
          s"move `mdocIn` somewhere else."
      )
    }
  }

  lazy val mdocJSWorkerClasspath = taskKey[Option[Seq[File]]](
    "Optional classpath to use for Mdoc.js worker - " +
      "if not provided, the classpath will be formed by resolving the worker dependency"
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      mdocIn := baseDirectory.in(ThisBuild).value / "docs",
      mdocOut := target.in(Compile).value / "mdoc",
      mdocVariables := Map.empty,
      mdocExtraArguments := Nil,
      mdocJS := None,
      mdocJSLibraries := Nil,
      mdocJSWorkerClasspath := None,
      mdocAutoDependency := true,
      mdocInternalVariables := Nil,
      mdoc := Def.inputTaskDyn {
        validateSettings.value
        val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed
        val args = Iterator(
          mdocExtraArguments.value,
          parsed
        ).flatten.mkString(" ")
        Def.taskDyn {
          runMain.in(Compile).toTask(s" mdoc.SbtMain $args")
        }
      }.evaluated,
      dependencyOverrides ++= List(
        "org.scala-lang" %% "scala3-library" % scalaVersion.value,
        "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
        "org.scala-lang" %% "tasty-core" % scalaVersion.value,
        "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
      ),
      libraryDependencies ++= {
        val isJS = mdocJS.value.isDefined
        if (mdocAutoDependency.value) {
          val suffix = if (isJS) "-js" else ""
          List("org.scalameta" %% s"mdoc$suffix" % BuildInfo.version)
        } else {
          List()
        }
      },
      resourceGenerators.in(Compile) += Def.task {
        val out =
          managedResourceDirectories.in(Compile).value.head / "mdoc.properties"
        val props = new java.util.Properties()
        mdocVariables.value.foreach { case (key, value) =>
          props.put(key, value)
        }
        mdocInternalVariables.value.foreach { case (key, value) =>
          props.put(key, value)
        }
        def getJars(mid: ModuleID) = {

          val depRes = dependencyResolution.in(update).value
          val updc = updateConfiguration.in(update).value
          val uwconfig = unresolvedWarningConfiguration.in(update).value
          val modDescr = depRes.wrapDependencyInModule(mid)

          depRes
            .update(
              modDescr,
              updc,
              uwconfig,
              streams.value.log
            )
            .map(_.allFiles)
            .fold(uw => throw uw.resolveException, identity)
        }

        val binaryVersion = scalaBinaryVersion.value
        val log = streams.value.log
        val libraries = mdocJSLibraries.value.map(_.data)
        val workerClasspathOverride = mdocJSWorkerClasspath.value

        mdocJSCompileOptions.value.foreach { options =>
          val sjsVersion = detectScalaJSVersion

          val linkerDependency = binaryVersion match {
            case "3" => "org.scala-js" % "scalajs-linker_2.13" % sjsVersion
            case other => "org.scala-js" % s"scalajs-linker_$other" % sjsVersion
          }

          val mdocJSDependency = binaryVersion match {
            case "3" => "org.scalameta" % "mdoc-js-worker_3" % BuildInfo.version
            case other => "org.scalameta" % s"mdoc-js-worker_$other" % BuildInfo.version
          }

          val workerClasspath = workerClasspathOverride.getOrElse(getJars(mdocJSDependency))

          MdocJSConfiguration(
            scalacOptions = options.options,
            compileClasspath = options.classpath,
            linkerClassPath = getJars(linkerDependency) ++ workerClasspath,
            moduleKind = options.moduleKind,
            jsLibraries = libraries
          ).writeTo(props)
        }
        props.put("in", mdocIn.value.toString)
        props.put("out", mdocOut.value.toString)
        props.put(
          "scalacOptions",
          scalacOptions.in(Compile).value.mkString(" ")
        )
        val classpath = ListBuffer.empty[File]
        // Can't use fullClasspath.value because it introduces cyclic dependency between
        // compilation and resource generation.
        classpath ++= dependencyClasspath.in(Compile).value.iterator.map(_.data)
        classpath += classDirectory.in(Compile).value
        props.put(
          "classpath",
          classpath.mkString(java.io.File.pathSeparator)
        )
        IO.write(props, "mdoc properties", out)
        List(out)
      }
    )

  case class MdocJSConfiguration(
      scalacOptions: Seq[String],
      compileClasspath: Seq[File],
      linkerClassPath: Seq[File],
      moduleKind: Option[String],
      jsLibraries: Seq[File]
  ) {
    def writeTo(props: java.util.Properties): Unit = {

      props.put(
        s"js-scalac-options",
        scalacOptions.mkString(" ")
      )
      props.put(
        s"js-classpath",
        compileClasspath.mkString(File.pathSeparator)
      )
      props.put(
        s"js-linker-classpath",
        linkerClassPath.mkString(File.pathSeparator)
      )
      props.put(
        s"js-libraries",
        jsLibraries.mkString(File.pathSeparator)
      )
      moduleKind.foreach { moduleKind => props.put(s"js-module-kind", moduleKind) }
    }
  }

  private lazy val mdocJSCompileOptions: Def.Initialize[Task[Option[CompileOptions]]] =
    Def.taskDyn[Option[CompileOptions]] {
      mdocJS.value match {
        case Some(p) =>
          mdocCompileOptions(p).map(Some(_))
        case None =>
          Def.task(None)
      }
    }
  private case class CompileOptions(
      options: Seq[String],
      classpath: Seq[File],
      moduleKind: Option[String]
  )
  private lazy val anyWriter = implicitly[sbt.util.OptJsonWriter[AnyRef]]
  // Loads a setting by fully qualified classname so that we don't have to depend on that sbt plugin directly.
  // Adapted from sbt-bloop sources: https://github.com/scalacenter/bloop/blob/ec217891ebd5190a0a66d6faf5bd000a6d951f3c/integrations/sbt-bloop/src/main/scala/bloop/integrations/sbt/SbtBloop.scala
  private def classloadedSetting(
      ref: Project,
      fullyQualifiedClassName: String,
      attributeName: String
  ): Def.Initialize[Option[AnyRef]] =
    Def.settingDyn {
      def proxyForSetting(): Def.Initialize[Option[AnyRef]] = {
        val cls = Class.forName(fullyQualifiedClassName)
        val stageManifest = new Manifest[AnyRef] { override def runtimeClass: Class[_] = cls }
        SettingKey(attributeName)(stageManifest, anyWriter).in(ref).?
      }
      try {
        val stageSetting = proxyForSetting()
        Def.setting {
          stageSetting.value
        }
      } catch {
        case _: ClassNotFoundException => Def.setting(None)
      }
    }
  private def mdocCompileOptions(ref: Project): Def.Initialize[Task[CompileOptions]] =
    Def.task {
      CompileOptions(
        scalacOptions.in(ref, Compile).value,
        fullClasspath.in(ref, Compile).value.map(_.data),
        classloadedSetting(
          ref,
          "org.scalajs.linker.interface.StandardConfig",
          "scalaJSLinkerConfig"
        ).value.map { config =>
          config.getClass.getMethod("moduleKind").invoke(config).toString
        }
      )
    }

  def detectScalaJSVersion = {
    val klass =
      Class.forName("org.scalajs.ir.ScalaJSVersions", true, getClass.getClassLoader())
    val method = klass.getMethod("current")
    method.invoke(null).asInstanceOf[String]
  }
}
