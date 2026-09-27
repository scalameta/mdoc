package mdoc.internal.markdown

import scala.meta._

object UserDefinedNames {
  def apply(sections: List[SectionInput]): Set[String] = {
    val names = Set.newBuilder[String]
    sections.foreach { section =>
      section.source.stats.foreach { stat =>
        names ++= topLevelDefinedNames(stat)
      }
    }
    names.result()
  }

  private def topLevelDefinedNames(stat: Tree): List[String] =
    stat match {
      case t: Defn.Val => t.pats.flatMap(binders).map(_.value)
      case t: Defn.Var => t.pats.flatMap(binders).map(_.value)
      case t: Defn.Def => List(t.name.value)
      case t: Defn.Object => List(t.name.value)
      case t: Defn.Class => List(t.name.value)
      case t: Defn.Trait => List(t.name.value)
      case t: Defn.Type => List(t.name.value)
      case _ => Nil
    }

  private def binders(pat: Pat): List[Name] =
    pat.collect { case m: Member => m.name }
}
