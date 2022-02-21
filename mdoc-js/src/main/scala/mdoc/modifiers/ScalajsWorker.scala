package mdoc.modifiers

import org.scalajs.linker.interface._
import java.net.{URL, URLClassLoader}
import scala.concurrent.Future
import java.nio.file.{Path, Paths}
import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import mdoc.internal.BuildInfo

case class ScalajsWorkerState(
    cachedIRFiles: Seq[IRFile],
    linker: ClearableLinker
)

class ScalajsWorker(compilerClasspath: Array[URL], claspath: Array[URL]) {
  def memOutputDirectory(): OutputDirectory =
    instMem.asInstanceOf[MemOutputFabric].apply()

  def readMemoryFile(directory: OutputDirectory, fileName: String): Option[Array[Byte]] =
    directory.asInstanceOf[MemOutputDirectoryFabric].content(fileName)

  def memIRFile(path: String, content: Array[Byte]): IRFile =
    klass
      .getConstructor(classOf[String], classOf[Option[String]], classOf[Array[Byte]])
      .newInstance(path, None, content)
      .asInstanceOf[IRFile]

  def newState(config: StandardConfig)(implicit ec: ExecutionContext): Future[ScalajsWorkerState] =
    for {
      irFiles <- pathIRContainer()
      cache = irCache.newCache
      clearableLinker = linker(config)
      cached <- cache.cached(irFiles)
    } yield ScalajsWorkerState(cached, clearableLinker)

  private final class FilteringClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
    private val parentPrefixes = List(
      "java.",
      "scala.",
      "org.scalajs.linker.interface.",
      "org.scalajs.logging.",
      "sun.reflect.",
      "jdk.internal.reflect."
    ) ++ scalaSpecificPrefixes

    private def scalaSpecificPrefixes =
      if (BuildInfo.scalaBinaryVersion != "3") List("org.scalajs.ir.") else Nil

    override def loadClass(name: String, resolve: Boolean): Class[_] = {
      if (parentPrefixes.exists(name.startsWith _))
        super.loadClass(name, resolve)
      else
        null
    }
  }

  private val classloader =
    new URLClassLoader(claspath, new FilteringClassLoader(getClass.getClassLoader()))

  import scala.reflect.runtime.{universe => ru}

  private val mirr = ru.runtimeMirror(classloader)
  private val inst = mirr
    .reflectModule(mirr.staticModule("org.scalajs.linker.StandardImpl"))
    .instance

  private val instMem = mirr
    .reflectModule(mirr.staticModule("org.scalajs.linker.MemOutputDirectory"))
    .instance

  private val instPathIR = mirr
    .reflectModule(mirr.staticModule("org.scalajs.linker.PathIRContainer"))
    .instance

  private val memIRFile = mirr.reflectClass(
    mirr.classSymbol(classloader.loadClass("org.scalajs.linker.standard.MemIRFileImpl"))
  )

  private val klass = classloader.loadClass("org.scalajs.linker.standard.MemIRFileImpl")
  private val klassOutput = classloader.loadClass("org.scalajs.linker.MemOutputDirectory")

  private type MemOutputFabric = {
    def apply(): OutputDirectory
  }

  private type MemOutputDirectoryFabric = {
    def content(name: String): Option[Array[Byte]]
  }

  private type LinkerFabric = {
    def linker(config: StandardConfig): Linker
    def clearableLinker(config: StandardConfig): ClearableLinker
    def irFileCache(): IRFileCache
  }

  private type PathIRContainerFabric = {
    def fromClasspath(classpath: Seq[Path])(implicit
        ec: ExecutionContext
    ): Future[(Seq[IRContainer], Seq[Path])]
  }

  private def linker(config: StandardConfig): ClearableLinker =
    inst.asInstanceOf[LinkerFabric].clearableLinker(config)

  private def irCache = inst.asInstanceOf[LinkerFabric].irFileCache()

  private def pathIRContainer()(implicit ec: ExecutionContext): Future[Seq[IRContainer]] = {
    instPathIR
      .asInstanceOf[PathIRContainerFabric]
      .fromClasspath(compilerClasspath.map(u => Paths.get(u.toURI)).toSeq)
      .map(_._1)
  }

}
