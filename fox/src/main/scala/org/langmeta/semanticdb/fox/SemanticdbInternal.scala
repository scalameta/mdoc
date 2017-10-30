package org.langmeta.semanticdb.fox

import scala.meta.Type
import org.langmeta.internal.semanticdb.{schema => s}
import org.langmeta.semanticdb.Denotation
import org.langmeta.semanticdb.HasFlags
object SemanticdbInternal {
  implicit class XtensionDenotationPretty(val d: Denotation) extends HasFlags {
    def flags: Long = d.flags
    def pretty: String = {
      if (!isDef) d.syntax
      else {
        val dialect = scala.meta.dialects.Scala212.copy(
          allowMethodTypes = true,
          allowTypeLambdas = true
        )
        def renderTypeMethod(tm: Type.Method) =
          tm.paramss
            .map(p => p.mkString("(", ", ", ")"))
            .mkString + ": " + tm.tpe.syntax
        val tpe = dialect(d.signature).parse[Type].get
        val info = tpe match {
          case tpe: Type.Method => renderTypeMethod(tpe)
          case Type.Lambda(tparams, tpe: Type.Method) =>
            tparams.mkString("[", ", ", "]" + renderTypeMethod(tpe))
          case e => ": " + e.syntax
        }
        s"$flagSyntax ${d.name}$info"
      }
    }
  }
  implicit class XtensionSchemaDenotation(val d: s.Denotation)
      extends HasFlags {
    override def flags: Long = d.flags
  }

}
