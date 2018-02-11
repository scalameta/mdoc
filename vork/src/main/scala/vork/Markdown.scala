package vork

import scala.language.dynamics

import java.nio.file.Path
import scala.annotation.tailrec
import scala.reflect.ClassTag
import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.NodeVisitor
import com.vladsch.flexmark.ast.VisitHandler
import com.vladsch.flexmark.ast.Visitor
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.CharSubSequence
import vork.markdown.VorkExtensions

object Markdown {
  case class Site(docs: List[Doc])
  case class Doc(path: Path, contents: String)
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

  def traverse[T <: Node](
      node: Node
  )(f: PartialFunction[T, Unit])(implicit ev: ClassTag[T]): Unit = {
    val lifted = f.lift
    val clazz = ev.runtimeClass.asInstanceOf[Class[T]]
    class Madness {
      val visitor = new NodeVisitor(new VisitHandler[T](clazz, visit))
      def visit(e: T): Unit = {
        lifted.apply(e)
        visitor.visitChildren(e)
      }
    }
    new Madness().visitor.visit(node)
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

  def parse(markdown: BasedSequence, settings: MutableDataSet): Node = {
    val parser = Parser.builder(settings).build
    parser.parse(markdown)
  }

  /**
    * Defines the default markdown settings.
    *
    * Do not use directly. The default flexmark settings have special keys set
    * up by vork to keep track of certain document-specific information like path.
    */
  def default(context: Context): MutableDataSet = {
    import com.vladsch.flexmark.parser.Parser
    // Scalac doesn't understand that it has to box the values, so we do it manually for primitives
    new MutableDataSet()
      .set(Parser.BLANK_LINES_IN_AST, Boolean.box(true))
      .set(Parser.LISTS_ITEM_INDENT, Integer.valueOf(1))
      .set(Parser.EXTENSIONS, VorkExtensions.default(context))
  }

  def toMarkdown(input: String, settings: MutableDataSet): String = {
    val parser = Parser.builder(settings).build
    val formatter = Formatter.builder(settings).build
    val ast = parser.parse(input)
    formatter.render(ast)
  }
}
