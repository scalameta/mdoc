package vork.internal.markdown

import scala.util.control.NoStackTrace
import vork.CustomModifier

final class CustomModifierException(mod: CustomModifier, cause: Throwable)
    extends Exception(mod.name, cause)
    with NoStackTrace
