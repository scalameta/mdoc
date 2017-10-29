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
    val emptyXml: xml.Node = xml.Text("")
//    pprint.log(index.packages)
    val docs = List.newBuilder[Doc]
    index.packages.foreach { pkg =>
      val url = pkg.syntax
      def render(data: SymbolData): xml.Node = {
        val children = data.publicMembers.map(render)
        val header =
          if (data.denotation.isClass ||
            data.denotation.isTrait ||
            data.denotation.isObject) {
            <h2>{data.denotation}</h2>
          } else if (data.denotation.isPackageObject) {
            <h2>package object {pkg.denotation.name}</h2>
          } else {
            <h3>{data.denotation}</h3>
          }

        <div>
          {header}
          <div style="margin-left: 1em">
            {children}
          </div>
        </div>
      }
      val members = pkg.members { m =>
        !m.denotation.isPackageObject &&
        !m.denotation.isPrivate
      }
      members.foreach(member => pprint.log(member.syntax))
      if (members.nonEmpty) {
        val pkgObject: xml.Node = pkg.packageObject.fold(emptyXml)(render)
        val content = xml.NodeSeq.fromSeq(pkgObject :: members.map(render))
        docs += Doc(
          Paths.get("api").resolve(s"$url.md"),
          url,
          header :: Nil,
          content.toString()
        )
      }
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
//    }
    docs.result()
  }

}
