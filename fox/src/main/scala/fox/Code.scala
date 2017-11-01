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

class Code(options: Options)(implicit index: Index) {

  def sources: Doc = {
    Doc(
      path = Paths.get("metadoc").resolve("index.md"),
      title = "Browse sources",
      headers = Nil,
      contents = ""
    )
  }

  // NOTE(olafur) this is super hacky, everything below needs to be re-written
  // for better organization.
  def api: List[Doc] = {
    val emptyXml: xml.Node = xml.Text("")
    val docs = List.newBuilder[Doc]
    index.packages.foreach { pkg =>
      val headers = List.newBuilder[Header]
      val url = pkg.syntax
      def render(level: Int)(data: SymbolData): xml.Node = {
        val members = data.members { m =>
          !m.denotation.isPrivate && {
            if (data.denotation.isTrait ||
              data.denotation.isObject ||
              data.denotation.isClass) {
              !m.denotation.isPrimaryCtor
            } else true
          }
        }
        if (data.denotation.isPackageObject && members.isEmpty) {
          emptyXml
        } else {
          // NOTE(olafur) this id will make overloaded methods conflict
          val id = data.syntax(pkg.symbol)
          headers += Header(data.header, id, level, data.docstring.getOrElse(""))
          val link = <a href={s"#$id"} class="headerlink">¶</a>
          val source = data.definition match {
            case pos @ m.Position.Range(m.Input.VirtualFile(path, _), _, _) =>
              val link =
                s"${options.baseUrl}/metadoc/#/$path#L${pos.startLine + 1}"
              <a href={link} class="fa fa-code" target="_blank" style="float: right"></a>
            case _ => emptyXml
          }
          val header =
            if (data.denotation.isTrait ||
              data.denotation.isClass ||
              data.denotation.isObject) {
              val signature =
                if (data.denotation.isClass) {
                  // NOTE(olafur) this will get very large for big classes.
                  // need to figure out line wrapping.
                  data
                    .members(_.denotation.isPrimaryCtor)
                    .headOption
                    .fold(data.signature)(
                      ctor => data.signature + ctor.denotation.signature
                    )
                } else
                  data.signature
              <h2 id={id}>{signature}{link}{source}</h2>
            } else {
              <h3 id={id}>{data.signature}{link}{source}</h3>
            }
          val children = members.map(render(level + 1)) match {
            case Nil => emptyXml
            case x =>
              <div style="margin-left: 1em">
                {x}
              </div>
          }

          <div>
            {header}
            {data.docstring.map(xml.Unparsed(_)).orNull}
            {children}
          </div>
        }
      }
      val members = pkg.members { m =>
        !m.denotation.isPackageObject &&
        !m.denotation.isPrivate
      }
      if (members.nonEmpty) {
        val pkgObject: xml.Node = pkg.packageObject.fold(emptyXml)(render(2))
        val content = xml.NodeSeq.fromSeq(pkgObject :: members.map(render(2)))
        docs += Doc(
          Paths.get("api").resolve(s"$url.md"),
          url,
          headers.result(),
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
object Code {
  def apply(options: Options): Code = {
    val runner = new MetadocRunner(
      options.classpathPaths,
      MetadocOptions(
        target = Some(options.outPath.resolve("metadoc").toString),
        cleanTargetFirst = false,
        zip = false,
        nonInteractive = true
      ),
      options
    )
    new Code(options)(runner.run())
  }
}
