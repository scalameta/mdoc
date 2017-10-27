package fox

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import com.rklaehn.radixtree.RadixTree
import com.rklaehn.sonicreducer.Reducer
import metadoc.schema.SymbolIndex
import metadoc.{schema => d}
import org.langmeta.internal.semanticdb.{schema => s}
import org.{langmeta => m}

case class CodeIndex(
    definitions: RadixTree[String, SymbolData],
    packages: RadixTree[String, SymbolData]
) {
  def data(symbol: m.Symbol): Option[SymbolData] =
    definitions.get(symbol.syntax)
}

object CodeIndex {
  def apply(
      files: ConcurrentHashMap[String, m.Input.VirtualFile],
      symbols: ConcurrentHashMap[String, AtomicReference[d.SymbolIndex]],
      denotations: ConcurrentHashMap[String, s.Denotation]
  ): CodeIndex = {
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
    val definitions: RadixTree[String, SymbolData] = {
      val reducer = Reducer[RadixTree[String, SymbolData]](_ merge _)
      symbols.asScala.foreach {
        case (
            symbolSyntax @ S(symbol: m.Symbol.Global, denot),
            R(SymbolIndex(_, Some(P(defn)), _))
            ) =>
          reducer.apply(
            RadixTree.singleton(
              symbolSyntax,
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
      reducer.resultOrElse(RadixTree.empty)
    }
    val packages: RadixTree[String, SymbolData] = {
      val reducer = Reducer[RadixTree[String, SymbolData]](_ merge _)
      val emptyPos = m.Position.Range(m.Input.None, -1, -1)
      denotations.asScala.foreach {
        case (symbolSyntax @ m.Symbol(symbol: m.Symbol.Global), sdenot) =>
          val denot = mdenot(sdenot)
          if (denot.isPackage) {
            reducer.apply(
              RadixTree.singleton(
                symbolSyntax,
                SymbolData(
                  symbol = symbol,
                  definition = emptyPos,
                  denotation = denot,
                  None
                )
              )
            )
          }
        case _ =>
      }
      reducer.resultOrElse(RadixTree.empty)
    }
    new CodeIndex(definitions, packages)
  }
}
