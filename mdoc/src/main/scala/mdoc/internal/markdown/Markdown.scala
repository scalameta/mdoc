package mdoc.internal.markdown

import scala.meta.internal.inputs._
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.formatter.Formatter
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.Visitor
import com.vladsch.flexmark.util.ast.VisitHandler
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
import com.vladsch.flexmark.util.sequence.BasedSequence
import java.nio.file.Files
import java.nio.file.Paths
import mdoc.Reporter
import mdoc.internal.cli.Context
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.internal.pos.PositionSyntax._
import scala.language.dynamics
import scala.meta.inputs.Input
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import scala.reflect.ClassTag

object Markdown {
  val InputKey = new DataKey[Option[Input]]("scalametaInput", None)
  val RelativePathKey = new DataKey[Option[RelativePath]]("mdocFile", None)
  val SiteVariables = new DataKey[Option[Map[String, String]]]("siteVariables", None)

  /** Defines the default markdown settings.
    *
    * Do not use directly. The default flexmark settings have special keys set up by mdoc to keep
    * track of certain document-specific information like path.
    */
  def mdocSettings(context: Context): MutableDataSet = {
    // Scalac doesn't understand that it has to box the values, so we do it manually for primitives
    val key: DataKey[java.util.Collection[Extension]] = Parser.EXTENSIONS
    val value: java.util.Collection[Extension] = MdocExtensions.mdoc(context).asJava
    baseSettings()
      .set(key, value)
      .set(SiteVariables, Some(context.settings.site))
  }

  def plainSettings(): MutableDataSet = {
    val key: DataKey[java.util.Collection[Extension]] = Parser.EXTENSIONS
    val value: java.util.Collection[Extension] = MdocExtensions.plain.asJava
    baseSettings()
      .set(key, value)
  }

  def baseSettings(): MutableDataSet = {
    new MutableDataSet()
      .set(Parser.BLANK_LINES_IN_AST, Boolean.box(true))
      .set(Parser.LISTS_ITEM_INDENT, Integer.valueOf(1))
  }

  def toDocument(
      input: Input,
      markdownSettings: MutableDataSet,
      reporter: Reporter,
      settings: Settings
  ): Document = {
    markdownSettings.set(InputKey, Some(input))
    RelativePathKey.get(markdownSettings) match {
      case None =>
        markdownSettings.set(
          RelativePathKey,
          Some(RelativePath(Paths.get(input.syntax).getFileName))
        )
      case _ =>
    }
    val variables = SiteVariables.get(markdownSettings).getOrElse(Map.empty)
    val textWithVariables = VariableRegex.replaceVariables(input, variables, reporter, settings)
    markdownSettings.set(InputKey, Some(textWithVariables))
    val parser = Parser.builder(markdownSettings).build
    val ast = parser.parse(textWithVariables.text)
    ast
  }

  def toHtml(
      input: Input,
      markdownSettings: MutableDataSet,
      reporter: Reporter,
      settings: Settings
  ): String = {
    val formatter = HtmlRenderer.builder(markdownSettings).build
    formatter.render(
      toDocument(
        input,
        markdownSettings,
        reporter,
        settings
      )
    )
  }

  final val DeprecatedInternal =
    "Methods in the `mdoc.internal` package are only intended to be used by mdoc itself so they are subject to breaking changes without notice. If you rely on this method, please open an issue to discuss how to expose it through the public mdoc library APIs https://github.com/scalameta/mdoc/"
  @deprecated(DeprecatedInternal, "2.2.0")
  def toMarkdown(
      input: Input,
      context: Context,
      relativePath: RelativePath,
      siteVariables: Map[String, String],
      reporter: Reporter,
      settings: Settings
  ): String = {
    throw new UnsupportedOperationException(DeprecatedInternal)
  }

  def toMarkdown(
      input: Input,
      context: Context,
      inputFile: InputFile,
      siteVariables: Map[String, String],
      reporter: Reporter,
      settings: Settings
  ): String = {
    val textWithVariables = VariableRegex.replaceVariables(
      input,
      siteVariables,
      reporter,
      settings
    )
    val file = MarkdownFile.parse(textWithVariables, inputFile, reporter)
    val processor = new Processor()(context)
    // TODO Or look into this processDocumet call?
    processor.processDocument(file)
    file.renderToString
  }

  def toMarkdown(
      input: Input,
      markdownSettings: MutableDataSet,
      reporter: Reporter,
      settings: Settings
  ): String = {
    val formatter = Formatter.builder(markdownSettings).build
    val document = toDocument(
      input,
      markdownSettings,
      reporter,
      settings
    )
    val appendable = new java.lang.StringBuilder()
    formatter.render(document, appendable)
    appendable.toString
  }

  def traverse[T <: Node](
      node: Node
  )(f: PartialFunction[T, Unit])(implicit ev: ClassTag[T]): Unit = {
    val lifted = f.lift
    val clazz = ev.runtimeClass.asInstanceOf[Class[T]]
    class Madness {
      val visitor = new NodeVisitor(new VisitHandler[T](clazz, new MadnessVisitor))
      class MadnessVisitor extends Visitor[T] {
        def visit(e: T): Unit = {
          lifted.apply(e)
          visitor.visitChildren(e)
        }
      }
    }
    new Madness().visitor.visit(node)
  }

  def collect[A <: Node: ClassTag, B](
      node: Node
  )(f: PartialFunction[A, B]): List[B] = {
    val lifted = f.lift
    val buffer = List.newBuilder[B]
    traverse[A](node) { case h =>
      lifted(h).foreach(buffer += _)
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
