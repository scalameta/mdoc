package fox

import java.io.File
import java.nio.file.Paths
import scala.{meta => m}
import fox.Markdown.Doc
import fox.Markdown.Header

class Code(options: Options) {
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
