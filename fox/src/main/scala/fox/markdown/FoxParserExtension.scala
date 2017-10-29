package fox.markdown

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Document, Heading, Node, Text}
import com.vladsch.flexmark.html.AttributeProvider
import com.vladsch.flexmark.html.CustomNodeRenderer
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory
import com.vladsch.flexmark.html.renderer.AttributablePart
import com.vladsch.flexmark.html.renderer.CoreNodeRenderer
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.{LinkRefProcessor, LinkRefProcessorFactory, Parser}
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.NodeTracker
import com.vladsch.flexmark.util.html.Attributes
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence, PrefixedSubSequence}
import fox.{Markdown, markdown}

object FoxHelpers {
  implicit def stringToCharSequence(string: String): BasedSequence = {
    CharSubSequence.of(string)
  }
}

class FoxParserExtension extends Parser.ParserExtension {
  class SiteVariableInjector(variables: Map[String, String], document: Document)
      extends LinkRefProcessor {
    /**
      * Creates a text node with the value of a site variable.
      *
      * Using `PrefixedSubSequence` and then removing suffix is the only way I found to simulate
      * `replace`. Converting from `String` to `BasedSequence` via `stringToCharSequence` does not
      * work because Flexmark's `Text` only uses the start and end offsets and does not do an
      * in-place replacement of the contents of the returned sequence. These start and end offsets
      * select from the original text, not the modified value that we wanted to insert.
      * Creating a custom node extending `ContentNode` or `CustomNode` did not help either.
      */
    override def createNode(nodeChars: BasedSequence): Node = {
      val key = nodeChars.midSequence(2, nodeChars.length() - 1).trim()
      val value = variables.getOrElse(key.unescape(), sys.error(s"Missing '$key' site variable."))
      new Text(PrefixedSubSequence.of(value, nodeChars).removeSuffix(nodeChars))
    }

    override def isMatch(nodeChars: BasedSequence): Boolean = {
      import FoxHelpers.stringToCharSequence
      // It has to be of the shape `![key]` and not `![key]()` or `![](key)` or `![][key]`
      nodeChars.countChars(nodeChars) > 3 &&
      nodeChars.startsWith("![") && nodeChars.endsWith("]") &&
      !nodeChars.containsAllOf("][")
    }

    override def adjustInlineText(doc: Document, node: Node): BasedSequence = node.getChars
    override def getBracketNestingLevel: Int = 0
    override def getWantExclamationPrefix: Boolean = true
    override def updateNodeElements(document: Document, node: Node): Unit = ()
    override def allowDelimiters(chars: BasedSequence, doc: Document, node: Node): Boolean = true
  }

  class SiteVariableInjectorFactory extends LinkRefProcessorFactory {
    override def getBracketNestingLevel(options: DataHolder): Int = 0
    override def getWantExclamationPrefix(options: DataHolder): Boolean = true
    override def create(document: Document): LinkRefProcessor =
      new SiteVariableInjector(Map("site.version" -> "1.0"), document)
  }

  override def extend(parserBuilder: Parser.Builder): Unit = {
    parserBuilder.linkRefProcessorFactory(new SiteVariableInjectorFactory)
    parserBuilder.postProcessorFactory(new markdown.FoxNodePostProcessor.Factory)
  }
  override def parserOptions(options: MutableDataHolder): Unit = ()
}

object FoxParserExtension {
  def create(): Extension = new FoxParserExtension
}

class FoxNodePostProcessor extends NodePostProcessor {
  import FoxHelpers.stringToCharSequence
  override def process(state: NodeTracker, node: Node): Unit = node match {
    case heading: Heading =>
      val link = new ast.Link()
      val text = new ast.Text("¶")
      val id = HeaderIdGenerator.generateId(heading.getText, null, false)
      link.setUrl(s"#$id")
      heading.appendChild(link)
      state.nodeAdded(link)
      link.appendChild(text)
      state.nodeAdded(text)
    case _ =>
  }
}
object FoxNodePostProcessor {
  class Factory extends NodePostProcessorFactory(false) {
    addNodes(classOf[Heading])
    override def create(document: Document): NodePostProcessor = {
      new FoxNodePostProcessor
    }
  }
}

class FoxAttributeProvider extends AttributeProvider {
  override def setAttributes(
      node: Node,
      part: AttributablePart,
      attributes: Attributes
  ): Unit = node match {
//    case p: ast.FencedCodeBlock =>
//      val old = Option(attributes.get("class")).fold("")(_.getValue)
//      attributes.replaceValue("class", "prettyprint prettyprinted " + old)
    case l: ast.Link =>
      if (l.getParent.isInstanceOf[Heading]) {
        attributes.replaceValue("class", "headerlink")
      }
    case h: Heading =>
      attributes.replaceValue(
        "id",
        HeaderIdGenerator.generateId(h.getText, null, false)
      )
    case _ =>
  }
}
object FoxAttributeProvider {
  class Factory extends IndependentAttributeProviderFactory {
    override def create(context: NodeRendererContext): AttributeProvider =
      new FoxAttributeProvider
  }
}

class FoxNodeRenderer extends NodeRenderer {
  override def getNodeRenderingHandlers: java.util.Set[NodeRenderingHandler[
    _ <: Node
  ]] = {
    val set = new java.util.HashSet[NodeRenderingHandler[_ <: Node]]
    set.add(
      new NodeRenderingHandler[ast.FencedCodeBlock](
        classOf[ast.FencedCodeBlock],
        new CustomNodeRenderer[ast.FencedCodeBlock]() {
          override def render(
              node: ast.FencedCodeBlock,
              context: NodeRendererContext,
              html: HtmlWriter
          ): Unit = {
            FoxNodeRenderer.this.render(node, context, html)
          }
        }
      )
    )
    set.add(
      new NodeRenderingHandler[ast.Paragraph](
        classOf[ast.Paragraph],
        new CustomNodeRenderer[ast.Paragraph]() {
          override def render(
              node: ast.Paragraph,
              context: NodeRendererContext,
              html: HtmlWriter
          ): Unit = {
            FoxNodeRenderer.this.renderParagraph(node, context, html)
          }
        }
      )
    )
    set.add(
      new NodeRenderingHandler[ast.IndentedCodeBlock](
        classOf[ast.IndentedCodeBlock],
        new CustomNodeRenderer[ast.IndentedCodeBlock]() {
          override def render(
              node: ast.IndentedCodeBlock,
              context: NodeRendererContext,
              html: HtmlWriter
          ): Unit = {
            FoxNodeRenderer.this.renderIndentedCodeBlock(node, context, html)
          }
        }
      )
    )
    set
  }

  private def renderIndentedCodeBlock(
      node: ast.IndentedCodeBlock,
      context: NodeRendererContext,
      html: HtmlWriter
  ): Unit = {
    Option(node.getPrevious) match {
      case Some(p: ast.Paragraph) if p.getChars.startsWith("!!!") => // nothing
      case _ =>
        context.delegateRender()
    }
  }

  private def renderParagraph(
      node: ast.Paragraph,
      context: NodeRendererContext,
      html: HtmlWriter
  ): Unit = {
    if (node.getChars.startsWith("!!!") &&
      node.getNext.isInstanceOf[ast.IndentedCodeBlock]) {
      val next = node.getNext.asInstanceOf[ast.IndentedCodeBlock]
      val (kind, title) = node.getChars.unescape().split(" ", 3).toList match {
        case _ :: kind :: title :: Nil =>
          kind -> title.stripSuffix("\"").stripPrefix("\"")
        case _ :: kind :: Nil => kind -> kind
        case _ => "unknown" -> node.getChars.toVisibleWhitespaceString
      }
      html.attr("class", s"admonition $kind")
      html.withAttr().tag("div")
      html.attr("class", "admonition-title")
      html.withAttr().tag("p")
      html.text(title)
      html.tag("/p")
      html.withAttr().tag("p")
      val codeblockChars =
        node.getNext.asInstanceOf[ast.IndentedCodeBlock].getContentChars
      // NOTE(olafur) this means links like `[a][b]` must define [b]: inside
      // the indented block.
      html.raw(Markdown.toHtml(codeblockChars))
      html.tag("/p")
      html.tag("/div")
    } else {
      context.delegateRender()
      node.getContentChars().trimTailBlankLines().normalizeEndWithEOL
    }
  }

  private def render(
      node: ast.FencedCodeBlock,
      context: NodeRendererContext,
      html: HtmlWriter
  ): Unit = {
    val language = node.getInfo
    // TODO(olafur) instead of using javascript highlighting, we can statically
    // generate it here with scalameta tokenizer.
    html.attr("class", "prettyprint")
    html
      .line()
      .srcPosWithEOL(node.getChars)
      .withAttr()
      .tag("pre")
      .openPre()
    html.attr("class", context.getHtmlOptions.languageClassPrefix + language)
    html
      .srcPosWithEOL(node.getContentChars)
      .withAttr(CoreNodeRenderer.CODE_CONTENT)
      .tag("code")
    context.renderChildren(node)
    html.tag("/code")
    html.tag("/pre").closePre()
    html.lineIf(context.getHtmlOptions.htmlBlockCloseTagEol)
  }
}
object FoxNodeRenderer {
  class Factory extends NodeRendererFactory {
    override def create(options: DataHolder): NodeRenderer = new FoxNodeRenderer
  }
}

class FoxAttributeProviderExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = ()
  override def extend(
      rendererBuilder: HtmlRenderer.Builder,
      rendererType: String
  ): Unit = {
    rendererBuilder.nodeRendererFactory(new FoxNodeRenderer.Factory)
    rendererBuilder.attributeProviderFactory(
      new FoxAttributeProvider.Factory
    )
  }
}
object FoxAttributeProviderExtension {
  def create() = new FoxAttributeProviderExtension
}
