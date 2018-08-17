package vork.internal.markdown

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.HtmlInline
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.parser.Parser
import java.net.URI
import java.nio.file.Paths
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.RelativePath
import scala.util.matching.Regex
import vork.Reporter
import vork.internal.cli.Settings

case class MarkdownReference(url: String, pos: Position) {
  def toAbsolute(enclosingDocument: String): String = {
    // Best-effort to reproduce link-generation in browsers and site-generators.
    // Probably wrong on many levels, will likely need more sophisticated
    if (url.startsWith("http") ||
      url.startsWith("/")) {
      url
    } else if (url.startsWith("#")) {
      enclosingDocument + url
    } else if (url.contains("..")) {
      URI.create(enclosingDocument + "/" + url).normalize().toString
    } else {
      url
    }
  }
  def isExternal: Boolean = url.startsWith("http")
  def isInternal: Boolean = !isExternal
}

case class MarkdownLinks(
    relpath: RelativePath,
    definitions: List[String],
    references: List[MarkdownReference]
) {
  def absoluteDefinitions: Set[String] = {
    val prefix = relpath.toURI(false).toString
    val buf = Set.newBuilder[String]
    buf += prefix
    definitions.foreach { defn =>
      buf += prefix + "#" + defn
    }
    buf.result()
  }
}
object MarkdownLinks {
  private def dashChars = " -_"
  // Poor man's html parser.
  private val HtmlName = "name=\"([^\"]+)\"".r
  private val HtmlId = "id=\"([^\"]+)\"".r

  def fromGeneratedSite(settings: Settings, reporter: Reporter): List[MarkdownLinks] = {
    val links = List.newBuilder[MarkdownLinks]
    val ls = FileIO.listAllFilesRecursively(settings.out)
    ls.files.foreach { relpath =>
      val isMarkdown = PathIO.extension(relpath.toNIO) == "md"
      val hasMatchingInputFile = settings.in.resolve(relpath).isFile
      if (isMarkdown && hasMatchingInputFile) {
        val abspath = ls.root.resolve(relpath)
        val input = Input.VirtualFile(relpath.toString(), FileIO.slurp(abspath, settings.charset))
        links += MarkdownLinks.fromMarkdown(relpath, input)
      }
    }
    links.result()
  }

  def fromMarkdown(relpath: RelativePath, input: Input): MarkdownLinks = {
    val settings = Markdown.plainSettings()
    val parser = Parser.builder(settings).build
    val ast = parser.parse(input.text)
    val definitions = List.newBuilder[String]
    val links = List.newBuilder[MarkdownReference]
    val reluri = relpath.toURI(false).toString
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
    MarkdownLinks(relpath, definitions.result(), links.result())
  }

}
