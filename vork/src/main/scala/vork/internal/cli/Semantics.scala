package vork.internal.cli

import metaconfig.ConfDecoder
import scalafix.internal.config.ReaderUtil

sealed trait Semantics
object Semantics {
  implicit val decoder: ConfDecoder[Semantics] =
    ReaderUtil.oneOf[Semantics](REPL, Script)
  case object REPL extends Semantics
  case object Script extends Semantics
}
