package mdoc.modifiers

import java.net.URL
import java.net.URLClassLoader

final class FilteringClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
  private val parentPrefixes = Array(
    "java.",
    "scala.",
    "org.scalajs.linker.",
    "org.scalajs.logging.",
    "sun.reflect.",
    "jdk.internal.reflect.",
    "mdoc.js."
  )

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (parentPrefixes.exists(name.startsWith _))
      super.loadClass(name, resolve)
    else
      null
  }
}

object ScalaJSClassloader {
  def create(classpath: Array[URL]): ClassLoader =
    new URLClassLoader(classpath, new FilteringClassLoader(getClass.getClassLoader()))

}
