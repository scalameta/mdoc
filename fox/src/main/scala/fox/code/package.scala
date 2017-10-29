package fox

import com.rklaehn.radixtree._
import com.rklaehn.radixtree.RadixTreeInternal._
import org.langmeta.semanticdb.Symbol
import org.langmeta.semanticdb.Signature

package object code {
  type SymbolKey = Array[Signature]
  type SymbolTable = RadixTree[SymbolKey, SymbolData]
  implicit class XtensionSymbolDataTableMembers(val data: SymbolData)
      extends AnyVal {
    def members(implicit index: Index): List[SymbolData] = data.symbol.members
  }
  implicit class XtensionSymbolTableMembers(val symbol: Symbol) extends AnyVal {
    def members(implicit index: Index): List[SymbolData] = {
      val buf = List.newBuilder[SymbolData]
      index.table.filterPrefix(symbol.toKey).internalChildren.foreach { rt =>
        rt.internalValue.foreach { value =>
          buf += value
        }
      }
      buf.result()
    }
  }
}
