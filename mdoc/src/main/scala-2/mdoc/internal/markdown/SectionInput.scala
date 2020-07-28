package mdoc.internal.markdown

import scala.meta.Source
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.Mod.Lazy
import mdoc.internal.pos.TokenEditDistance
import mdoc.internal.cli.{Context => MContext}

case class SectionInput(input: Input, source: Source, mod: Modifier)

object SectionInput {

  def tokenEdit(sections: List[SectionInput], instrumented: Input): TokenEditDistance = {
    TokenEditDistance.fromTrees(sections.map(_.source), instrumented)
  }

  def apply(input: Input, context: MContext): SectionInput = {
    val source = MdocDialect.scala(input).parse[Source].getOrElse(Source(Nil))
    SectionInput(input, source, Modifier.Default())
  }
}
