package fox

import scala.meta._
import fox.code.Index
import org.langmeta.semanticdb.fox.SemanticdbInternal._

case class SymbolData(
    symbol: Symbol.Global,
    definition: Position,
    denotation: Denotation,
    docstring: Option[Token.Comment]
) {
  override def toString: String = syntax
  def signature(implicit index: Index): String = {
    if (denotation.isPackageObject) s"package object ${denotation.name}"
    else if (denotation.isObject) s"object ${denotation.name}"
    else if (denotation.isDef) s"${denotation.pretty}"
    else denotation.toString()
  }
  def header: String = {
    // very ugly but better than nothing :D
    val prefix =
      if (denotation.isObject) "Ⓞ "
      else if (denotation.isTrait) "Ⓣ "
      else if (denotation.isClass) "Ⓒ "
      else ""
    s"$prefix${denotation.name}"
  }
  def syntax: String = syntax(Symbol.None)
  def syntax(prefix: Symbol): String = {
    val sb = new java.lang.StringBuilder()
    def loop(s: Symbol): Unit = s match {
      case `prefix` =>
      case Symbol.Global(Symbol.None, Signature.Term("_root_")) =>
      case Symbol.Global(owner, signature) =>
        loop(owner)
        if (s eq symbol) sb.append(signature.name)
        else sb.append(signature.syntax)
      case _ => sb.append(s.syntax)
    }
    loop(symbol)
    sb.toString
  }
  def isOwner(other: SymbolData): Boolean =
    // TODO(olafur): hack
    other.symbol.syntax.startsWith(symbol.syntax)
  def filename: String = definition.input match {
    case Input.VirtualFile(path, _) => path
    case _ => "<none>"
  }
  def prettyDenotation: Denotation =
    if (denotation.isPrimaryCtor || denotation.isSecondaryCtor)
      denotation.copy(name = "this")
    else if (denotation.isPackageObject) denotation.copy(name = "this")
    else denotation
}
