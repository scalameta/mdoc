package mdoc.internal.markdown

import scala.util.control.NoStackTrace
import mdoc.StringModifier

final class StringModifierException(mod: StringModifier, cause: Throwable)
    extends Exception(mod.name, cause)
    with NoStackTrace
