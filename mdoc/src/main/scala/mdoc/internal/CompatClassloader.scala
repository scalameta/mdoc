package mdoc.internal

import java.net.URLClassLoader
import java.net.URL
import sun.misc.Unsafe
import mdoc.internal.pos.PositionSyntax._

object CompatClassloader {

  /** Utility to get SystemClassLoader/ClassLoader urls in java8 and java9+ Based upon:
    * https://gist.github.com/hengyunabc/644f8e84908b7b405c532a51d8e34ba9
    */
  def getURLs(classLoader: ClassLoader): Seq[URL] = {
    if (classLoader.isInstanceOf[URLClassLoader]) {
      classLoader.asInstanceOf[URLClassLoader].getURLs().toIndexedSeq
      // java9+
    } else if (
      classLoader
        .getClass()
        .getName()
        .startsWith("jdk.internal.loader.ClassLoaders$")
    ) {
      try {
        val field = classOf[Unsafe].getDeclaredField("theUnsafe")
        field.setAccessible(true)
        val unsafe = field.get(null).asInstanceOf[Unsafe]

        // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        val ucpField = {
          if (System.getProperty("java.version").split("\\.")(0).toInt >= 16) {
            // the `ucp` field is  not in `AppClassLoader` anymore, but in `BuiltinClassLoader`
            classLoader.getClass().getSuperclass()
          } else {
            classLoader.getClass()
          }
        }.getDeclaredField("ucp")
        val ucpFieldOffset: Long = unsafe.objectFieldOffset(ucpField)
        val ucpObject = unsafe.getObject(classLoader, ucpFieldOffset)

        // jdk.internal.loader.URLClassPath.path
        val pathField = ucpField.getType().getDeclaredField("path")
        val pathFieldOffset = unsafe.objectFieldOffset(pathField)
        val paths: Seq[URL] = unsafe
          .getObject(ucpObject, pathFieldOffset)
          .asInstanceOf[java.util.ArrayList[URL]]
          .asScala
          .toSeq

        paths
      } catch {
        case ex: Exception =>
          ex.printStackTrace()
          Nil
      }
    } else {
      Nil
    }
  }
}
