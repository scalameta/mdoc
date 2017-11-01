package com.rklaehn.radixtree

import cats.kernel.Order
import org.langmeta.semanticdb.Symbol
import org.langmeta.semanticdb.Signature

object RadixTreeInternal {
  implicit class XtensionRadixTree[A, B](val tree: RadixTree[A, B])
      extends AnyVal {
    def internalValue: Option[B] = {
      val opt = tree.valueOpt
      if (opt.isDefined) Some(opt.get)
      else None
    }
    def internalChildren: Array[RadixTree[A, B]] = tree.children
  }
  implicit val signatureOrder: Order[Signature] = SignatureOrder
  private object SignatureOrder extends Order[Signature] {
    override def compare(x: Signature, y: Signature): Int =
      x.name.compareTo(y.name)
  }
  implicit val signatureHash: Hash[Signature] = SignatureHash
  private object SignatureHash extends Hash[Signature] {
    override def hash(a: Signature): Int = a.hashCode()
    override def eqv(x: Signature, y: Signature): Boolean = x == y
  }
  implicit class XtensionSymbolTableKey(val symbol: Symbol) extends AnyVal {
    def toKey: Array[Signature] = {
      val buf = Array.newBuilder[Signature]
      def loop(s: Symbol): Unit = s match {
        case Symbol.None =>
        case Symbol.Global(owner, signature) =>
          loop(owner)
          buf += signature
        case _ => // ???
      }
      loop(symbol)
      buf.result()
    }
  }

}
