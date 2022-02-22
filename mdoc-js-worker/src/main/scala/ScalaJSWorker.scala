package mdoc.js.worker;

import mdoc.js.api._
import java.nio.file.Path
import org.scalajs.linker.MemOutputDirectory
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.linker.StandardImpl
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.interface.ModuleKind
import ModuleType._
import org.scalajs.linker.PathIRContainer
import org.scalajs.linker.interface.IRFile
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.{util => ju}
import mdoc.js.api.ScalajsWorkerApi.OutputDirectory
import org.scalajs.logging.Logger
import org.scalajs.logging.Level
import org.scalajs.linker.standard.MemIRFileImpl

class ScalaJSWorker(config: ScalajsConfig) extends ScalajsWorkerApi {
  case class Oot(mem: MemOutputDirectory) extends ScalajsWorkerApi.OutputDirectory
  case class IFile(mem: IRFile) extends ScalajsWorkerApi.IRFile

  val linker = {
    val cfg =
      StandardConfig()
        .withBatchMode(config.batchMode)
        .withClosureCompilerIfAvailable(config.closureCompiler)
        .withSourceMap(config.sourceMap)
        .withModuleKind {
          config.moduleType match {
            case NoModule => ModuleKind.NoModule
            case ESModule => ModuleKind.ESModule
            case CommonJSModule => ModuleKind.CommonJSModule
          }
        }
    StandardImpl.clearableLinker(cfg)
  }

  var cachedFiles = Seq.empty[org.scalajs.linker.interface.IRFile]

  val cache = StandardImpl.irFileCache().newCache

  override def newFolder(): ScalajsWorkerApi.OutputDirectory = Oot(MemOutputDirectory())

  override def cache(x: Array[Path]): Unit =
    cachedFiles = Await.result(
      PathIRContainer
        .fromClasspath(x.toSeq)
        .map(_._1)
        .flatMap(cache.cached),
      Duration.Inf
    )
  val loger = new Logger {
    override def log(level: Level, message: => String): Unit = {
      // if (level >= config.minLevel) {
      //   if (level == Level.Warn) reporter.info(message)
      //   else if (level == Level.Error) reporter.info(message)
      //   else reporter.info(message)
      // }
    }

    override def trace(t: => Throwable): Unit = ()
    // reporter.error(t)
  }

  override def link(
      in: Array[ScalajsWorkerApi.IRFile]
  ): ju.Map[String, Array[Byte]] = {
    val mem = MemOutputDirectory()
    val report = Await.result(
      linker.link(
        cachedFiles.toSeq ++
          in.toSeq.collect { case IFile(o) =>
            o
          },
        Seq.empty,
        mem,
        loger
      ),
      Duration.Inf
    )

    val javaMap: ju.Map[String, Array[Byte]] = new ju.HashMap[String, Array[Byte]]

    report.publicModules.foreach { m =>
      mem.content(m.jsFileName).foreach { content =>
        javaMap.put(m.jsFileName, content)
      }
    }

    javaMap
  }

  override def inMemory(path: String, contents: Array[Byte]): ScalajsWorkerApi.IRFile = IFile(
    new MemIRFileImpl(path, None, contents)
  )

}
