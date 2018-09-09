package mdoc.internal.markdown

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.HtmlInline
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.parser.Parser
import java.net.URI
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.RelativePath
import scala.util.matching.Regex
import mdoc.Reporter
import mdoc.internal.cli.Settings

case class MarkdownReference(url: String, pos: Position) {
  def isExternal: Boolean = url.startsWith("http") || url.startsWith("//")
  def isInternal: Boolean = !isExternal
}

case class DocumentLinks(
    relpath: RelativePath,
    definitions: List[String],
    references: List[MarkdownReference]
) {
  def absoluteDefinitions: Set[URI] = {
    val prefix = relpath.toURI(false)
    val buf = Set.newBuilder[URI]
    buf += prefix
    definitions.foreach { defn =>
      try {
        buf += prefix.resolve("#" + defn)
      } catch {
        case _: IllegalArgumentException => // Ignore, invalid URI
      }
    }
    buf.result()
  }
}

object DocumentLinks {
  private def dashChars = " -_"
  // Poor man's html parser.
  private val HtmlName = "name=\"([^\"]+)\"".r
  private val HtmlId = "id=\"([^\"]+)\"".r

  def fromGeneratedSite(settings: Settings, reporter: Reporter): List[DocumentLinks] = {
    val links = List.newBuilder[DocumentLinks]
    val ls = FileIO.listAllFilesRecursively(settings.out)
    ls.files.foreach { relpath =>
      val isMarkdown = PathIO.extension(relpath.toNIO) == "md"
      val hasMatchingInputFile = settings.in.resolve(relpath).isFile
      if (isMarkdown && hasMatchingInputFile) {
        val abspath = ls.root.resolve(relpath)
        val input = Input.VirtualFile(relpath.toString(), FileIO.slurp(abspath, settings.charset))
        links += DocumentLinks.fromMarkdown(relpath, input)
      }
    }
    links.result()
  }

  def fromMarkdown(relpath: RelativePath, input: Input): DocumentLinks = {
    val settings = Markdown.plainSettings()
    val parser = Parser.builder(settings).build
    val ast = parser.parse(input.text)
    val definitions = List.newBuilder[String]
    val links = List.newBuilder[MarkdownReference]
    Markdown.foreach(ast) {
      case heading: Heading =>
        definitions += HeaderIdGenerator.generateId(heading.getText, dashChars, false)
      case p: Paragraph if p.getChars.startsWith("<a name=") =>
        p.getChars.toString.lines.next() match {
          case HtmlName(id) =>
            definitions += id
          case _ =>
        }
      case h: HtmlInline if !h.getChars.startsWith("</") =>
        def handleRegex(r: Regex): Unit =
          r.findAllMatchIn(h.getChars).foreach { m =>
            definitions += m.group(1)
          }
        handleRegex(HtmlId)
        handleRegex(HtmlName)
      case link: Link =>
        val pos = Position.Range(input, link.getStartOffset, link.getEndOffset)
        links += MarkdownReference(link.getUrl.toString, pos)
      case els =>
        ()
    }
    DocumentLinks(relpath, definitions.result(), links.result())
  }

}
