package mdoc.js.worker;

import mdoc.js.interfaces._
import java.nio.file.Path
import org.scalajs.ir
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
import org.scalajs.logging.Logger
import org.scalajs.logging.Level
import org.scalajs.linker.standard.MemIRFileImpl
import org.scalajs.linker.interface.Semantics
import scala.jdk.CollectionConverters._
import com.armanbilge.sjsimportmap.ImportMappedIRFile

class ScalaJSWorker(
    config: ScalajsConfig,
    logger: Logger
) extends ScalajsWorkerApi {
  case class IFile(mem: IRFile) extends ScalajsWorkerApi.IRFile

  val linker = {
    val cfg =
      StandardConfig()
        .withSemantics {
          if (config.fullOpt) Semantics.Defaults.optimized
          else Semantics.Defaults
        }
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

  logger.info(s"scalaJSWorker")
  logger.info(config.importMap.toString)

  lazy val remapFct = config.importMap.asScala.toSeq.foldLeft((in: String) => in) {
    case (fct, (s1, s2)) =>
      val fct2: (String => String) = (in => in.replace(s1, s2))
      (in => fct(fct2(in)))
  }

  var cachedFiles = Seq.empty[org.scalajs.linker.interface.IRFile]

  val cache = StandardImpl.irFileCache().newCache

  override def cache(x: Array[Path]): Unit =
    cachedFiles = Await.result(
      PathIRContainer
        .fromClasspath(x.toSeq)
        .map(_._1)
        .flatMap(cache.cached),
      Duration.Inf
    )

  override def link(
      in: Array[ScalajsWorkerApi.IRFile]
  ): ju.Map[String, Array[Byte]] = {
    val mem = MemOutputDirectory()
    val report = Await.result(
      linker.link(
        (
          cachedFiles.toSeq ++
            in.toSeq
              .collect { case IFile(o) =>
                o
              }
        ).map { ir =>
          if (config.importMap.isEmpty)
            ir
          else
            ImportMappedIRFile.fromIRFile(ir)(remapFct)
        },
        Seq.empty,
        mem,
        logger
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
    new MemIRFileImpl(path, ir.Version.Unversioned, contents)
  )

}
