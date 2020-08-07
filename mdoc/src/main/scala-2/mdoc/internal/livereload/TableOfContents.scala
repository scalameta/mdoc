package mdoc.internal.livereload

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.util.ast.Document
import mdoc.internal.markdown.Markdown
import scala.collection.mutable.ListBuffer

case class TableOfContents(
    title: String,
    id: String,
    level: Int,
    parent: Option[TableOfContents],
    children: ListBuffer[TableOfContents] = ListBuffer.empty[TableOfContents]
) {
  def toHTML(fromLevel: Int, toLevel: Int, indent: String): String = {
    def childrenHTML = {
      s"""|$indent<ul>
          |${children.map(_.toHTML(fromLevel, toLevel, indent + "  ")).mkString("\n")}
          |$indent</ul>""".stripMargin
    }
    def titleHTML = s"""$indent<li><a href="#$id">$title</a></li>"""
    if (level < fromLevel) {
      childrenHTML
    } else if (level > toLevel) {
      ""
    } else {
      titleHTML + "\n" + childrenHTML
    }
  }
  def addChild(heading: Heading): TableOfContents = {
    addChild(heading.getText.toString, heading.getAnchorRefId, heading.getLevel)
  }
  def addChild(childTitle: String, childId: String, level: Int): TableOfContents = {
    val child = TableOfContents(childTitle, childId, level, parent = Some(this))
    children += child
    child
  }
}

object TableOfContents {
  def apply(doc: Document): TableOfContents = {
    val root = TableOfContents("Root", "root", level = 0, parent = None)
    var toc = root
    Markdown.foreach(doc) {
      case heading: Heading =>
        while (toc.level >= heading.getLevel && toc.parent.isDefined) {
          toc = toc.parent.get
        }
        toc = toc.addChild(heading)
      case _ =>
    }
    root
  }
}
