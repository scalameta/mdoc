package vork.markdown.processors

import java.io.File
import java.net.URLClassLoader
import java.net.URLDecoder
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter
import metaconfig.ConfError
import metaconfig.Configured
import org.langmeta.inputs.Input
import org.langmeta.inputs.Position
import vork.runtime.Document
import vork.runtime.DocumentBuilder

object MarkdownCompiler {
  def default(): MarkdownCompiler = new MarkdownCompiler(defaultClasspath)

  def document(compiler: MarkdownCompiler): Document = {
    val code =
      """
        |package vork
        |class Generated extends runtime.DocumentBuilder {
        |  def app(): Unit = {
        |      statement {
        |        val y = List(1, 2).map(_ + 1); binder(y);
                 statement { section { () } }
        |      }
        |  }
        |}
      """.stripMargin
    val loader = compiler.compile(Input.String(code)).get
    val cls = loader.loadClass("vork.Generated")
    cls.newInstance().asInstanceOf[DocumentBuilder].build()
  }

  // Copy paste from scalafix
  def defaultClasspath: String = {
    getClass.getClassLoader match {
      case u: URLClassLoader =>
        val paths = u.getURLs.toList.map(u => {
          if (u.getProtocol.startsWith("bootstrap")) {
            import java.io._
            import java.nio.file._
            val stream = u.openStream
            val tmp = File.createTempFile("bootstrap-" + u.getPath, ".jar")
            Files.copy(stream, Paths.get(tmp.getAbsolutePath), StandardCopyOption.REPLACE_EXISTING)
            tmp.getAbsolutePath
          } else {
            URLDecoder.decode(u.getPath, "UTF-8")
          }
        })
        paths.mkString(File.pathSeparator)
      case _ => ""
    }
  }
}

class MarkdownCompiler(
    classpath: String,
    target: AbstractFile = new VirtualDirectory("(memory)", None)
) {
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  settings.classpath.value = classpath
  lazy val reporter = new StoreReporter
  private val global = new Global(settings, reporter)
  private val classLoader =
    new AbstractFileClassLoader(target, this.getClass.getClassLoader)

  def compile(input: Input): Configured[ClassLoader] = {
    reporter.reset()
    val run = new global.Run
    val label = input match {
      case Input.File(path, _) => path.toString()
      case Input.VirtualFile(path, _) => path
      case _ => "(input)"
    }
    run.compileSources(List(new BatchSourceFile(label, new String(input.chars))))
    val errors = reporter.infos.collect {
      case reporter.Info(pos, msg, reporter.ERROR) =>
        ConfError
          .message(msg)
          .atPos(
            if (pos.isDefined) Position.Range(input, pos.start, pos.end)
            else Position.None
          )
          .notOk
    }
    ConfError
      .fromResults(errors.toSeq)
      .map(_.notOk)
      .getOrElse(Configured.Ok(classLoader))
  }
}
