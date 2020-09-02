package mdoc.internal.markdown

import scala.util.control.NoStackTrace

final class ModifierException(mod: String, cause: Throwable)
    extends Exception(s"mdoc:$mod exception", cause)
    with NoStackTrace
