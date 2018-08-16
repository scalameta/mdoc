package vork.internal.markdown

import scala.util.control.NoStackTrace
import vork.StringModifier

final class StringModifierException(mod: StringModifier, cause: Throwable)
    extends Exception(mod.name, cause)
    with NoStackTrace
