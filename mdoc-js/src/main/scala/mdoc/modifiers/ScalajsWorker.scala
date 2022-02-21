package mdoc.modifiers

import java.net.{URL, URLClassLoader}
import scala.concurrent.Future
import java.nio.file.{Path, Paths}
import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import mdoc.internal.BuildInfo
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import org.scalajs.logging.Logger
import scala.collection.immutable

case class ScalajsWorkerState(
    cachedIRFiles: Seq[_IRFile],
    linker: _Linker
)

sealed abstract class ModuleKind(val nm: String)
object ModuleKind {
  case object NoModule extends ModuleKind("ModuleKind$NoModule$")
  case object ESModule extends ModuleKind("ModuleKind$ESModule$")
  case object CommonJSModule extends ModuleKind("ModuleKind$CommonJSModule$")
}

class ScalajsWorker(compilerClasspath: Array[URL], claspath: Array[URL]) {
  def memOutputDirectory(): _MemOutputDirectory =
    new _MemOutputDirectory(
      classes.objectMemOutputDirectory.getMethod("apply").invoke(null),
      classes
    )

  def readMemoryFile(directory: _MemOutputDirectory, fileName: String): Option[Array[Byte]] =
    directory.content(fileName)

  def memIRFile(path: String, content: Array[Byte]): _IRFile =
    new _IRFile(
      classes.klassMemIRFileImpl
        .getConstructor(classOf[String], classOf[Option[String]], classOf[Array[Byte]])
        .newInstance(path, None, content),
      classes
    )

  def newState(
      config: _StandardConfig
  )(implicit ec: ExecutionContext): Future[ScalajsWorkerState] = {
    for {
      irFiles <- pathIRContainer()
      cache = irCache.newCache
      clearableLinker = linker(config)
      cached <- cache.cached(irFiles)
    } yield ScalajsWorkerState(cached, clearableLinker)
  }

  def emptyConfig: _StandardConfig = new _StandardConfig(classes)

  private final class FilteringClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
    private val parentPrefixes = List(
      "java.",
      "scala.",
      "org.scalajs.linker.",
      "org.scalajs.logging.",
      "sun.reflect.",
      "jdk.internal.reflect."
    )

    override def loadClass(name: String, resolve: Boolean): Class[_] = {
      if (parentPrefixes.exists(name.startsWith _))
        super.loadClass(name, resolve)
      else
        null
    }
  }

  private val classloader =
    new URLClassLoader(claspath, new FilteringClassLoader(getClass.getClassLoader()))

  private implicit val classes: Classes = new Classes(classloader)

  private def linker(config: _StandardConfig): _Linker =
    new _StandardImpl(classes).linker(config)

  private def irCache = new _StandardImpl(classes).irFileCache

  private def pathIRContainer()(implicit ec: ExecutionContext): Future[Seq[_IRContainer]] = {
    new _PathIRContainer(classes)
      .fromClassPath(compilerClasspath.map(u => Paths.get(u.toURI)).toSeq)
  }

}

class _StandardConfig(classes: Classes) { self =>
  private val kls = classes.klassStandardConfig
  private var impl =
    classes.klassStandardConfig.getConstructor().newInstance().asInstanceOf[java.lang.Object]

  def withModuleKind(kind: ModuleKind) = {
    impl = kls
      .getMethod("withModuleKind", classes.linkerClass("interface.ModuleKind"))
      .invoke(impl, classes.linkerObject("interface." + kind.nm).getField("MODULE$").get(null))
    self
  }

  def withOptimized(enable: Boolean) = {
    val defaults = {
      val kls = classes.linkerObject("interface.Semantics$")

      val module = kls
        .getField("MODULE$")
        .get(null)

      kls.getMethod("Defaults").invoke(module)
    }

    val klsSemantics = classes.linkerClass("interface.Semantics")

    if (!enable)
      impl = kls.getMethod("withSemantics", klsSemantics).invoke(impl, defaults)
    else {
      impl = kls
        .getMethod("withSemantics", klsSemantics)
        .invoke(impl, klsSemantics.getMethod("optimized").invoke(defaults))
    }

    self
  }

  private def flag(name: String, value: Boolean) = {
    impl = kls
      .getMethod("withSourceMap", classOf[Boolean])
      .invoke(impl, java.lang.Boolean.valueOf(value))
    self
  }

  def withSourceMap(enable: Boolean) =
    flag("withSourceMap", enable)

  def withBatchMode(enable: Boolean) =
    flag("withBatchMode", enable)

  def withClosureCompilerIfAvailable(enable: Boolean) =
    flag("withClosureCompilerIfAvailable", enable)

  def get = impl
}

class Classes(classloader: ClassLoader) {
  def linkerClass(nm: String): Class[_] =
    Class.forName("org.scalajs.linker." + nm, false, classloader)
  def linkerObject(nm: String): Class[_] =
    Class.forName("org.scalajs.linker." + nm, true, classloader)

  val klassLinker = linkerClass("interface.Linker")
  val klassReport = linkerClass("interface.Report")
  val klassModule = linkerClass("interface.Report$Module")
  val klassStandardImpl =
    Class.forName("org.scalajs.linker.StandardImpl", true, classloader)
  val klassStandardConfig =
    Class.forName("org.scalajs.linker.interface.StandardConfig", false, classloader)
  val klassIRFileCache =
    Class.forName("org.scalajs.linker.interface.IRFileCache", false, classloader)
  val klassIRCache =
    Class.forName("org.scalajs.linker.interface.IRFileCache$Cache", false, classloader)
  val klassIRContainer =
    Class.forName("org.scalajs.linker.interface.IRContainer", false, classloader)
  val klassMemIRFileImpl =
    Class.forName("org.scalajs.linker.standard.MemIRFileImpl", false, classloader)
  val objectMemOutputDirectory =
    linkerObject("MemOutputDirectory")
  val klassMemOutputDirectory =
    linkerClass("MemOutputDirectory")
  val klassPathIRContainer =
    Class.forName("org.scalajs.linker.PathIRContainer", true, classloader)

  val klassOutputDirectory = linkerClass("interface.OutputDirectory")
}

class _Linker(loaded: Any, classes: Classes) {
  private val linkMethod = classes.klassLinker.getMethod(
    "link",
    classOf[Seq[Any]], // irFiles
    classOf[Seq[Any]], // moduleInitializers
    classes.klassOutputDirectory,
    classOf[Logger],
    classOf[ExecutionContext]
  )
  def link(files: Seq[_IRFile], output: _MemOutputDirectory, logger: Logger)(implicit
      ec: ExecutionContext
  ): Future[_Report] = {
    linkMethod
      .invoke(loaded, files.map(_.loaded), Seq.empty, output.loaded, logger, ec)
      .asInstanceOf[Future[Any]]
      .map(r => new _Report(r, classes))
  }
}
class _IRFile(private[modifiers] val loaded: Any, classes: Classes)

class _IRFileCache(loaded: Any, classes: Classes) {
  private val newCacheMethod = classes.klassIRFileCache.getMethod("newCache")

  def newCache: _IRCache = new _IRCache(newCacheMethod.invoke(loaded), classes)
}
class _IRCache(loaded: Any, classes: Classes) {
  private val cachedMethod =
    classes.klassIRCache.getMethod("cached", classOf[Seq[Any]], classOf[ExecutionContext])
  def cached(files: Seq[_IRContainer])(implicit ec: ExecutionContext): Future[Seq[_IRFile]] =
    cachedMethod
      .invoke(loaded, files.map(_.loaded), ec)
      .asInstanceOf[Future[Seq[Any]]]
      .map(_.map(f => new _IRFile(f, classes)))
}
class _IRContainer(private[modifiers] val loaded: Any, classes: Classes)
class _MemOutputDirectory(private[modifiers] val loaded: Object, classes: Classes) {
  private val contentMethod = classes.klassMemOutputDirectory.getMethod("content", classOf[String])

  def content(name: String): Option[Array[Byte]] =
    contentMethod.invoke(loaded, name).asInstanceOf[Option[Array[Byte]]]
}

class _StandardImpl(classes: Classes) {
  private val linkerMethod =
    classes.klassStandardImpl.getMethod("linker", classes.klassStandardConfig)
  private val irFileCacheMethod = classes.klassStandardImpl.getMethod("irFileCache")

  def irFileCache: _IRFileCache = new _IRFileCache(irFileCacheMethod.invoke(null), classes)
  def linker(inst: _StandardConfig) = {
    new _Linker(linkerMethod.invoke(null, inst.get), classes)
  }
}

class _Report(loaded: Any, classes: Classes) {
  private val publicModulesMethod = classes.klassReport.getMethod("publicModules")
  def publicModules: List[_Module] = publicModulesMethod
    .invoke(loaded)
    .asInstanceOf[immutable.Iterable[Any]]
    .map(m => new _Module(m, classes))
    .toList
}

class _Module(loaded: Any, classes: Classes) {
  private val jsFileNameMethod = classes.klassModule.getMethod("jsFileName")
  def jsFileName: String = jsFileNameMethod.invoke(loaded).asInstanceOf[String]
}

class _PathIRContainer(classes: Classes) {
  private val fromMethod =
    classes.klassPathIRContainer.getMethod(
      "fromClasspath",
      classOf[Seq[Any]],
      classOf[ExecutionContext]
    )

  def fromClassPath(
      classpath: Seq[Path]
  )(implicit ec: ExecutionContext): Future[Seq[_IRContainer]] =
    fromMethod
      .invoke(null, classpath, ec)
      .asInstanceOf[Future[(Seq[Any], Seq[Any])]]
      .map(_._1)
      .map { sq =>
        sq.map(a => new _IRContainer(a, classes))
      }

}
