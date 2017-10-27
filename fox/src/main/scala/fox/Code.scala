package fox

import java.io.File
import java.nio.file.Paths
import scala.{meta => m}
import caseapp.RemainingArgs
import fox.Markdown.Doc
import fox.Markdown.Header
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions

class Code(options: Options) {
  def metadoc: Doc = {
    MetadocCli.run(
      MetadocOptions(
        target = Some(options.outPath.resolve("metadoc").toString),
        cleanTargetFirst = false,
        zip = false,
        nonInteractive = true
      ),
      RemainingArgs(
        remainingArgs = options.classpath,
        Nil
      )
    )

    Doc(
      Paths.get("metadoc").resolve("index.md"),
      "Browse sources",
      Nil,
      "",
      renderFile = false
    )
  }
  def api: Doc = {
    val cp = m.Classpath(options.classpath.mkString(File.pathSeparator))
    val db = m.Database.load(cp)
    pprint.log(cp)
    pprint.log(db.toString())
    val names = db.names.collect {
      case m.ResolvedName(_, sym: m.Symbol.Global, true) =>
        sym
    }
    val content =
      <ul>{names.sortBy(_.signature.name).reverseIterator.map(n => <li>{n.signature.name}</li>)}</ul>
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
