package fox

import scala.language.dynamics

import java.nio.file.Path
import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag
import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.NodeVisitor
import com.vladsch.flexmark.ast.VisitHandler
import com.vladsch.flexmark.ast.Visitor
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.CharSubSequence

object Markdown {
  case class Site(docs: List[Doc], sources: Doc, api: List[Doc]) {
    def all: Traversable[Doc] = new Traversable[Doc] {
      override def foreach[U](f: Doc => U): Unit = {
        docs.foreach(f)
        api.foreach(f)
      }
    }
  }
  case class Doc(
      path: Path,
      title: String,
      headers: List[Header],
      contents: String,
      renderFile: Boolean = true // HACK! false only for metadoc
  )
  case class Header(title: String, id: String, level: Int, text: String) {
    def target: String = if (level == 1) "" else s"#$id"
  }
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
  // If you can figure out how to fix errors like here below than we can remove
  // this unary_! hack:
  // [error] [E1] fox/src/main/scala/fox/Markdown.scala
  // [error]      type mismatch;
  // [error]       found   : Class[_$1] where type _$1
  // [error]       required: Class[_ <: T]
  // [error]      L48:    new NodeVisitor(new VisitHandler[T](ev.runtimeClass, new Visitor[T] {
  // [error]      L48:                                           ^
  // [error] fox/src/main/scala/fox/Markdown.scala: L48 [E1]
  private implicit class XtensionBang[A](val a: A) extends AnyVal {
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

  def toHtml(markdown: BasedSequence): String = {
    val s = new java.lang.StringBuilder()
    markdown.appendTo(s)
    val settings = default
    val parser = Parser.builder(settings).build
    val renderer = HtmlRenderer.builder(settings).build
    val document = parser.parse(markdown)
    renderer.render(document)
  }
  def toHtml(markdown: String): String = {
    toHtml(CharSubSequence.of(markdown))
  }

  def default: MutableDataSet = {
    import com.vladsch.flexmark.parser.Parser
    val options = new MutableDataSet()
    options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
    import scala.collection.JavaConverters._
    options.set(
      Parser.EXTENSIONS,
      Iterable(
        AutolinkExtension.create(),
        markdown.FoxParserExtension.create(),
        markdown.FoxAttributeProviderExtension.create()
      ).asJava
    )
  }

}
