package scalamd

import java.nio.file.Path
import scala.annotation.tailrec
import scala.reflect.ClassTag
import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.NodeVisitor
import com.vladsch.flexmark.ast.VisitHandler
import com.vladsch.flexmark.ast.Visitor
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.util.options.MutableDataSet

object Markdown {
  case class Doc(path: Path, headers: List[Header], body: ast.Document)
  case class Header(title: String, id: String, level: Int, text: String)
  object Header {
    def apply(h: Heading): Header = {
      val text = new java.lang.StringBuilder
      @tailrec def loop(n: Node): Unit = n match {
        case null =>
        case h: ast.Heading => ()
        case t =>
          t.getChildChars.appendTo(text)
          text.append("\n")
          loop(t.getNext)
      }
      loop(h.getNext)
      Header(
        h.getText.toVisibleWhitespaceString,
        HeaderIdGenerator.generateId(h.getText, null, true),
        h.getLevel,
        text.toString
      )
    }
  }
  implicit class XtensionBang[A](val a: A) extends AnyVal {
    def unary_![B]: B = a.asInstanceOf[B]
  }

  def traverse[T <: Node](
      node: Node
  )(f: PartialFunction[T, Unit])(implicit ev: ClassTag[T]): Unit = {
    val lifted = f.lift
    new NodeVisitor(new VisitHandler[T](!ev.runtimeClass, new Visitor[T] {
      override def visit(node: T): Unit = lifted.apply(node)
    })).visit(node)
  }

  def collect[A <: Node: ClassTag, B](
      node: Node
  )(f: PartialFunction[A, B]): List[B] = {
    val lifted = f.lift
    val buffer = List.newBuilder[B]
    traverse[A](node) {
      case h => lifted(h).foreach(buffer += _)
    }
    buffer.result()
  }
  def default: MutableDataSet = {
    import com.vladsch.flexmark.parser.Parser
    val options = new MutableDataSet()
    options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    import scala.collection.JavaConverters._
    options.set(!HtmlRenderer.GENERATE_HEADER_ID, true)
    options.set[Object](
      !Parser.EXTENSIONS,
      Iterable(AnchorLinkExtension.create()).asJava
    )
    options.set(!AnchorLinkExtension.ANCHORLINKS_SET_ID, true)
    options.set(!AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false)
    options.set(!AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "scalamd-header")
  }
}
