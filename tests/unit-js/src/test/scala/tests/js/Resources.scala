package tests.js

import java.nio.file.Files
import java.nio.file.Path

object Resources {

  /** A resource as a file on disk.
    *
    * sbt 2 puts test resources in a jar; extract to file, to obtain Path.
    */
  def asPath(name: String): Path = {
    val in = getClass.getClassLoader.getResourceAsStream(name)
    require(in != null, s"no such resource: $name")
    try {
      val out = Files.createTempFile("mdoc-", "-" + name.replace('/', '-'))
      Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      out.toAbsolutePath()
    } finally in.close()
  }
}
