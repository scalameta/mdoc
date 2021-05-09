package mdoc.internal.markdown

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.HtmlInline
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.parser.Parser
import java.net.URI
import mdoc.Reporter
import mdoc.internal.cli.Settings
import mdoc.internal.io.IO
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.RelativePath
import scala.util.matching.Regex

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
    IO.foreachOutput(settings) { (abspath, relpath) =>
      val isMarkdown = PathIO.extension(relpath.toNIO) == "md"
      if (isMarkdown) {
        val input = Input.VirtualFile(relpath.toString(), FileIO.slurp(abspath, settings.charset))
        links += DocumentLinks.fromMarkdown(settings.headerIdGenerator, relpath, input)
      }
    }
    links.result()
  }

  def fromMarkdown(
      headerIdGenerator: String => String,
      relpath: RelativePath,
      input: Input
  ): DocumentLinks = {
    val markdownSettings = Markdown.plainSettings()
    val parser = Parser.builder(markdownSettings).build
    val ast = parser.parse(input.text)
    val definitions = List.newBuilder[String]
    val links = List.newBuilder[MarkdownReference]
    Markdown.foreach(ast) {
      case heading: Heading =>
        definitions += headerIdGenerator(heading.getText.toString)
      case p: Paragraph if p.getChars.startsWith("<a name=") =>
        p.getChars.toString.linesIterator.next() match {
          case HtmlName(id) =>
            definitions += id
          case _ =>
        }
      case h: HtmlInline if !h.getChars.startsWith("</") =>
        def handleRegex(r: Regex): Unit =
          r.findAllMatchIn(h.getChars).foreach { m => definitions += m.group(1) }
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
