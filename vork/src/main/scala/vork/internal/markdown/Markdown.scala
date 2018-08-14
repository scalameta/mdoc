package vork.internal.markdown

import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.NodeVisitor
import com.vladsch.flexmark.ast.VisitHandler
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence
import scala.language.dynamics
import scala.meta.inputs.Input
import scala.reflect.ClassTag
import vork.Reporter
import vork.internal.cli.Context
import vork.internal.cli.MainOps

object Markdown {

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
      .set(MainOps.VariablesKey, Some(context.settings.site))
  }

  def toMarkdown(input: Input.VirtualFile, settings: MutableDataSet, reporter: Reporter): String = {
    val variables = settings.get(MainOps.VariablesKey).getOrElse(Map.empty)
    val textWithVariables = SiteVariableRegexp.replaceVariables(input, variables, reporter)
    settings.set(MainOps.InputKey, Some(textWithVariables))
    val parser = Parser.builder(settings).build
    val formatter = Formatter.builder(settings).build
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

  def parse(markdown: BasedSequence, settings: MutableDataSet): Node = {
    val parser = Parser.builder(settings).build
    parser.parse(markdown)
  }

}
