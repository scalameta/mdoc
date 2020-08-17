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

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      mdocIn := baseDirectory.in(ThisBuild).value / "docs",
      mdocOut := target.in(Compile).value / "mdoc",
      mdocVariables := Map.empty,
      mdocExtraArguments := Nil,
      mdocJS := None,
      mdocJSLibraries := Nil,
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
          runMain.in(Compile).toTask(s" mdoc.Main $args")
        }
      }.evaluated,
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
        mdocVariables.value.foreach {
          case (key, value) =>
            props.put(key, value)
        }
        mdocInternalVariables.value.foreach {
          case (key, value) =>
            props.put(key, value)
        }
        mdocJSCompileOptions.value.foreach { options =>
          props.put(
            s"js-scalac-options",
            options.options.mkString(" ")
          )
          props.put(
            s"js-classpath",
            options.classpath.mkString(File.pathSeparator)
          )
          options.moduleKind.foreach { moduleKind => props.put(s"js-module-kind", moduleKind) }
        }
        props.put(
          s"js-libraries",
          mdocJSLibraries.value.map(_.data).mkString(File.pathSeparator)
        )
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
          "org.scalajs.core.tools.linker.backend.ModuleKind",
          "scalaJSModuleKind"
        ).value.map(_.toString)
      )
    }

}
