package fox

import java.nio.file.Paths
import scala.{meta => m}
import fox.Markdown.Doc

class Code(options: Options) {
  def overview: Doc = {
    val db =
      m.Database.load(m.Classpath(options.classpath.map(m.AbsolutePath(_))))
    val names = db.names.collect {
      case m.ResolvedName(_, sym, true) =>
        <li>{sym.syntax}</li>
    }
    val content = <ul>{names}</ul>
    Doc(Paths.get("code").resolve("index.md"), "Codedocs", Nil, content.toString())
  }

}
