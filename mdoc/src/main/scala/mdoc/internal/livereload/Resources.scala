package mdoc.internal.livereload

import java.nio.charset.StandardCharsets
import scala.meta.internal.io.InputStreamIO

object Resources {
  def readPath(path: String): String = {
    val is = this.getClass.getResourceAsStream(path)
    if (is == null) {
      throw new NoSuchElementException(path)
    }
    val bytes =
      try InputStreamIO.readBytes(is)
      finally is.close()
    val text = new String(bytes, StandardCharsets.UTF_8)
    text
  }
}
