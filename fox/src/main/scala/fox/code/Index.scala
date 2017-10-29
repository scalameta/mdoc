package fox.code

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import cats.kernel.Order
import com.rklaehn.radixtree.Hash
import com.rklaehn.radixtree.RadixTree
import com.rklaehn.sonicreducer.Reducer
import fox.SymbolData
import metadoc.schema.SymbolIndex
import metadoc.{schema => d}
import org.langmeta.internal.semanticdb.{schema => s}
import org.langmeta.semanticdb.Signature
import org.{langmeta => m}
import com.rklaehn.radixtree.RadixTreeInternal._

case class Index(
    table: SymbolTable,
    packages: List[SymbolData]
)

object Index {

  def apply(
      files: ConcurrentHashMap[String, m.Input.VirtualFile],
      symbols: ConcurrentHashMap[String, AtomicReference[d.SymbolIndex]],
      denotations: ConcurrentHashMap[String, s.Denotation]
  ): Index = {
    object R {
      def unapply[T](arg: AtomicReference[T]): Option[T] =
        Option(arg.get)
    }

    def mdenot(sdenot: s.Denotation): m.Denotation =
      m.Denotation(sdenot.flags, sdenot.name, sdenot.signature, Nil)

    object S {
      def unapply(sym: String): Option[(m.Symbol, m.Denotation)] =
        for {
          sdenot <- Option(denotations.get(sym))
          denot = mdenot(sdenot)
          symbol <- m.Symbol.unapply(sym)
        } yield symbol -> denot
    }
    object P {
      def unapply(defn: d.Position): Option[m.Position.Range] =
        for {
          input <- Option(files.get(defn.filename))
        } yield m.Position.Range(input, defn.start, defn.end)
    }
    import scala.collection.JavaConverters._
    val packagesB = List.newBuilder[m.Symbol.Global]
    val table: SymbolTable = {
      val reducer = Reducer[SymbolTable](_ merge _)
      symbols.asScala.foreach {
        case (
            S(symbol: m.Symbol.Global, denot),
            R(SymbolIndex(_, Some(P(defn)), _))
            ) =>
          reducer.apply(
            RadixTree.singleton(
              symbol.toKey,
              SymbolData(
                symbol = symbol,
                definition = defn,
                denotation = denot,
                None
              )
            )
          )
        case _ =>
      }
      denotations.asScala.foreach {
        case (m.Symbol(symbol: m.Symbol.Global), sdenot) =>
          val denot = mdenot(sdenot)
          if (denot.isPackage) {
            packagesB += symbol
          }
          reducer.apply(
            RadixTree.singleton(
              symbol.toKey,
              SymbolData(
                symbol,
                m.Position.None,
                denot,
                None
              )
            )
          )
        case _ =>
      }
      reducer.resultOrElse(RadixTree.empty)
    }
    val packages = packagesB.result().flatMap(pkg => table.get(pkg.toKey))
    new Index(table, packages)
  }
}
