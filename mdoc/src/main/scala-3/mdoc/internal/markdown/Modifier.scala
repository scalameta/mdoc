package mdoc.internal.markdown

import mdoc.internal.markdown.Mod._

/* Can be removed once the project is updated once metaconfig is released for Scala 3
 * originally Modifier depends on some code that uses metaconfig, which uses macros 
 * and doesn't work with Scala 3
 * */
sealed abstract class Modifier(val mods: Set[Mod]) 

object Modifier {
  object Default {
    def apply(): Modifier = Builtin(Set.empty)
  }

  case class Builtin(override val mods: Set[Mod]) extends Modifier(mods)

}
