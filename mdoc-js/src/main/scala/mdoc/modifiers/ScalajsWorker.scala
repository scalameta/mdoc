package mdoc.modifiers

import org.scalajs.linker.interface._
import java.net.{URL, URLClassLoader}
import scala.concurrent.Future
import java.nio.file.{Path, Paths}
import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls

class ScalajsWorkerState(
    cachedIRFiles: Seq[IRFile],
    linker: ClearableLinker
)

class ScalajsWorker(compilerClasspath: Array[URL], claspath: Array[URL]) {
  private val shared = getClass().getClassLoader()

  private final class FilteringClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
    private val parentPrefixes = List(
      "java.",
      "scala.",
      "org.scalajs.linker.",
      "org.scalajs.logging.",
      "org.scalajs.ir.",
      "sun.reflect.",
      "jdk.internal.reflect."
    )

    private val forbiddenFruit = List("org.scalajs.linker.interface")

    override def loadClass(name: String, resolve: Boolean): Class[_] = {
      if (parentPrefixes.exists(name.startsWith _))
        super.loadClass(name, resolve)
      else
        null
    }
  }

  private val classloader = new URLClassLoader(claspath, ClassLoader.getPlatformClassLoader()) {
    val toShared = Set("org.scalajs.linker.interface", "scala.", "java.", "org.scalajs.logging")
    override def findClass(name: String): Class[_] =
      if (toShared.exists(name.startsWith))
        shared.loadClass(name)
      else super.findClass(name)
  }

  // private val classloader =
  //   new URLClassLoader(claspath, new FilteringClassLoader(ClassLoader.getPlatformClassLoader()))

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

  def linker(config: StandardConfig): ClearableLinker =
    inst.asInstanceOf[LinkerFabric].clearableLinker(config)
  def memOutputDirectory(): OutputDirectory =
    instMem.asInstanceOf[MemOutputFabric].apply()

  def memIRFile(path: String, content: Array[Byte]): IRFile =
    klass
      .getConstructor(classOf[String], classOf[Option[String]], classOf[Array[Byte]])
      .newInstance(path, None, content)
      .asInstanceOf[IRFile]

  def irCache = inst.asInstanceOf[LinkerFabric].irFileCache()

  def pathIRContainer()(implicit ec: ExecutionContext): Future[Seq[IRContainer]] = {
    instPathIR
      .asInstanceOf[PathIRContainerFabric]
      .fromClasspath(compilerClasspath.map(u => Paths.get(u.toURI)).toSeq)
      .map(_._1)
  }
}
