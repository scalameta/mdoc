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
    val names = symbols.map(_.symbol)
    val content = <ul>{names.map(n => <li>{n.signature.name}</li>)}</ul>
    val header =
      Header("Symbols", "symbols", 1, names.map(_.signature.name).mkString(" "))
    Doc(
      Paths.get("api").resolve("index.md"),
      "API reference",
      header :: Nil,
      content.toString()
    )
  }

}
