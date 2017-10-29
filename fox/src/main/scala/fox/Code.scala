package fox

import fox.code._
import java.nio.file.Paths
import scala.{meta => m}
import caseapp.RemainingArgs
import fox.Markdown.Doc
import fox.Markdown.Header
import fox.code.Index
import metadoc.cli.MetadocOptions
import metadoc.{schema => d}
import org.langmeta.internal.semanticdb.{schema => s}

class Code(options: Options) {
  def docs: List[Doc] = {
    val runner = new MetadocRunner(
      options.classpathPaths,
      MetadocOptions(
        target = Some(options.outPath.resolve("metadoc").toString),
        cleanTargetFirst = false,
        zip = false,
        nonInteractive = true
      )
    )
    sources :: api(runner.run())
  }

  private def sources: Doc = {
    Doc(
      path = Paths.get("metadoc").resolve("index.md"),
      title = "Browse sources",
      headers = Nil,
      contents = "",
      renderFile = false
    )
  }

  private def api(implicit index: Index): List[Doc] = {
    val search = new StringBuilder
    val header = Header("Symbols", "symbols", 1, search.toString())
//    pprint.log(index.packages)
    val docs = List.newBuilder[Doc]
    index.packages.foreach { pkg =>
      pprint.log(pkg.syntax)
      pkg.members.foreach(member => pprint.log(member.syntax))
    }

//    index.packages.values.toList.map { pkg =>
//      val url = pkg.syntax
//      val content = """"""
//      pprint.log(pkg.symbol.syntax)
//      index.definitions
//        .filterPrefix(pkg.symbol.syntax)
//        .values
//        .foreach { defn =>
//          pprint.log(defn.syntax)
//          pprint.log(defn.enclosingPackage.syntax)
//        }
//      Doc(
//        Paths.get("api").resolve(s"$url.md"),
//        url,
//        header :: Nil,
//        content.toString()
//      )
//    }
    docs.result()
  }

}
