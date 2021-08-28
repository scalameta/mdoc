package mdoc.internal.markdown

import scala.meta.Source
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.Mod.Lazy
import mdoc.internal.pos.TokenEditDistance
import mdoc.internal.cli.{Context => MContext}
import scala.meta.parsers.Parsed.Success
import mdoc.internal.pos.PositionSyntax._

case class SectionInput(input: Input, source: ParsedSource, mod: GenModifier)

object SectionInput {

  def tokenEdit(sections: List[SectionInput], instrumented: Input): TokenEditDistance = {
    TokenEditDistance.fromTrees(sections.map(_.source.source), instrumented)
  }

  def apply(input: Input, context: MContext): SectionInput = {
    apply(input, Modifier.Default(), context)
  }
  def apply(input: Input, mod: Modifier, context: MContext): SectionInput = {
    val source: Source = (input, MdocDialect.scala).parse[Source] match {
      case parsers.Parsed.Success(source) =>
        source
      case parsers.Parsed.Error(pos, msg, _) =>
        context.reporter.error(pos.toUnslicedPosition, msg)
        Source(Nil)
    }
    SectionInput(input, ParsedSource(source), mod)
  }
}
