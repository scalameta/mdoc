package fox.utils

import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured

object Decoders {
  implicit val CharsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.flatMap { str =>
      try {
        Configured.ok(Charset.forName(str))
      } catch {
        case _: UnsupportedCharsetException =>
          ConfError.message(s"Charset '$str' is unsupported").notOk
        case _: IllegalCharsetNameException =>
          ConfError.message(s"Charset name '$str' is illegal").notOk
      }
    }

  implicit val PathDecoder: ConfDecoder[Path] =
    ConfDecoder.stringConfDecoder.flatMap { str =>
      try {
        Configured.ok(Paths.get(str))
      } catch {
        case e: InvalidPathException =>
          ConfError.message(e.getMessage).notOk
      }
    }

}
