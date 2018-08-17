package vork.internal.markdown

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.NodeVisitor
import com.vladsch.flexmark.ast.VisitHandler
import com.vladsch.flexmark.ext.anchorlink.AnchorLink
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataSet
import scala.collection.JavaConverters._
import com.vladsch.flexmark.util.sequence.BasedSequence
import scala.language.dynamics
import scala.meta.inputs.Input
import scala.reflect.ClassTag
import vork.Reporter
import vork.internal.cli.Context
import vork.internal.cli.Settings
import com.vladsch.flexmark.parser.Parser

object Markdown {
  val InputKey = new DataKey[Option[Input]]("scalametaInput", None)
  val SiteVariables = new DataKey[Option[Map[String, String]]]("siteVariables", None)

  /**
    * Defines the default markdown settings.
    *
    * Do not use directly. The default flexmark settings have special keys set
    * up by vork to keep track of certain document-specific information like path.
    */
  def vorkSettings(context: Context): MutableDataSet = {
    // Scalac doesn't understand that it has to box the values, so we do it manually for primitives
    baseSettings()
      .set(Parser.EXTENSIONS, VorkExtensions.vork(context).asJava)
      .set(SiteVariables, Some(context.settings.site))
  }

  def plainSettings(): MutableDataSet = {
    baseSettings()
      .set(Parser.EXTENSIONS, VorkExtensions.plain.asJava)
  }

  def baseSettings(): MutableDataSet = {
    new MutableDataSet()
      .set(Parser.BLANK_LINES_IN_AST, Boolean.box(true))
      .set(Parser.LISTS_ITEM_INDENT, Integer.valueOf(1))
  }

  def toMarkdown(
      input: Input,
      markdownSettings: MutableDataSet,
      reporter: Reporter,
      settings: Settings
  ): String = {
    markdownSettings.set(InputKey, Some(input))
    val variables = markdownSettings.get(SiteVariables).getOrElse(Map.empty)
    val textWithVariables = VariableRegex.replaceVariables(input, variables, reporter, settings)
    markdownSettings.set(InputKey, Some(textWithVariables))
    val parser = Parser.builder(markdownSettings).build
    val formatter = Formatter.builder(markdownSettings).build
    val ast = parser.parse(textWithVariables.text)
    formatter.render(ast)
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

  def foreach(node: Node)(fn: Node => Unit): Unit = {
    fn(node)
    node.getChildren.asScala.foreach(child => foreach(child)(fn))
  }

  def parse(markdown: BasedSequence, settings: MutableDataSet): Node = {
    val parser = Parser.builder(settings).build
    parser.parse(markdown)
  }

}
