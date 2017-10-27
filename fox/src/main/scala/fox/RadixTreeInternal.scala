package com.rklaehn.radixtree

object RadixTreeInternal {
  implicit class XtensionRadixTree[A, B](val tree: RadixTree[A, B]) extends AnyVal {
    def internalChildren: Array[RadixTree[A, B]] = tree.children
  }
}
