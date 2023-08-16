package mdoc.internal

import java.net.URLClassLoader
import java.net.URL
import mdoc.internal.pos.PositionSyntax._

object CompatClassloader {

  abstract class Unsafe {
    def objectFieldOffset(field: java.lang.reflect.Field): Long
    def getObject(obj: AnyRef, offset: Long): AnyRef
  }

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
          val unsafeClass = classLoader.loadClass("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.setAccessible(true)
        val unsafe = field.get(null)

        // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        val ucpField = {
          if (System.getProperty("java.version").split("\\.")(0).toInt >= 16) {
            // the `ucp` field is  not in `AppClassLoader` anymore, but in `BuiltinClassLoader`
            classLoader.getClass().getSuperclass()
          } else {
            classLoader.getClass()
          }
        }.getDeclaredField("ucp")
        
        def objectFieldOffset(field: java.lang.reflect.Field): Long =
          unsafeClass
            .getMethod(
              "objectFieldOffset",
              classOf[java.lang.reflect.Field]
            )
            .invoke(unsafe, field)
            .asInstanceOf[Long]

        def getObject(obj: AnyRef, offset: Long): AnyRef =
          unsafeClass
            .getMethod(
              "getObject",
              classOf[AnyRef],
              classOf[Long]
            )
            .invoke(unsafe, obj, offset.asInstanceOf[AnyRef])
            .asInstanceOf[AnyRef]

        val ucpFieldOffset = objectFieldOffset(ucpField)
        val ucpObject = getObject(classLoader, ucpFieldOffset)

        val pathField = ucpField.getType().getDeclaredField("path")        
        val pathFieldOffset = objectFieldOffset(pathField)
        val paths: Seq[URL] = getObject(ucpObject, pathFieldOffset)
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
