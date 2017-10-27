package fox

import java.io.File
import java.nio.file.Paths
import scala.{meta => m}
import caseapp.RemainingArgs
import fox.Markdown.Doc
import fox.Markdown.Header
import metadoc.{schema => d}
import metadoc.cli.MetadocOptions

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
    val index = runner.run()
    sources :: api(index) :: Nil
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

  private def api(index: MetadocIndex): Doc = {
    val symbols = index.symbolData
    val search = new StringBuilder
    def render(d: SymbolData): xml.Node = {
      search
        .append(d.symbol.signature.name)
        .append(' ')
      if (d.denotation.name == "<init>") xml.Text("")
      else if (d.denotation.isDef ||
        d.denotation.isVal ||
        d.denotation.isVar) <p>{d.denotation.toString()}</p>
      else <h2>{d.denotation.toString()}</h2>
    }

    val content = xml.NodeSeq.fromSeq(symbols.map(render))
    val header = Header("Symbols", "symbols", 1, search.toString())
    Doc(
      Paths.get("api").resolve("index.md"),
      "API reference",
      header :: Nil,
      content.toString()
    )
  }

}
