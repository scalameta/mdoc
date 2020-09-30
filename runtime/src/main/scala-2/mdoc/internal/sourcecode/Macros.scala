package mdoc.internal.sourcecode

import language.experimental.macros

trait StatementMacro {
  implicit def generate[T](v: T): SourceStatement[T] = macro Macros.text[T]
  def apply[T](v: T): SourceStatement[T] = macro Macros.text[T]

}

object Macros {

  def text[T: c.WeakTypeTag](c: Compat.Context)(v: c.Expr[T]): c.Expr[SourceStatement[T]] = {
    import c.universe._
    val fileContent = new String(v.tree.pos.source.content)
    val start = v.tree.collect { case treeVal =>
      treeVal.pos match {
        case NoPosition ⇒ Int.MaxValue
        case p ⇒ p.startOrPoint
      }
    }.min
    val g = c.asInstanceOf[reflect.macros.runtime.Context].global
    val parser = g.newUnitParser(fileContent.drop(start))
    parser.expr()
    val end = parser.in.lastOffset
    val txt = fileContent.slice(start, start + end)
    val tree = q"""${c.prefix}(${v.tree}, $txt)"""
    c.Expr[SourceStatement[T]](tree)
  }
}
