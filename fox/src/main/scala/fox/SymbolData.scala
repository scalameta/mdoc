package fox

import scala.meta._

case class SymbolData(
    symbol: Symbol.Global,
    definition: Position.Range,
    denotation: Denotation,
    docstring: Option[Token.Comment]
)
