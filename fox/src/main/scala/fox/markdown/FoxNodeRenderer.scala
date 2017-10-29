package fox.markdown

import fox.{Markdown, Options}

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.html.renderer.{CoreNodeRenderer, NodeRenderer, NodeRendererContext, NodeRendererFactory, NodeRenderingHandler}
import com.vladsch.flexmark.html.{CustomNodeRenderer, HtmlWriter}
import com.vladsch.flexmark.util.options.{DataHolder, MutableDataSet}

class FoxNodeRenderer(options: Options) extends NodeRenderer {
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
      html.raw(Markdown.toHtml(codeblockChars, options))
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
  class Factory(options: Options) extends NodeRendererFactory {
    override def create(mdOptions: DataHolder): NodeRenderer = new FoxNodeRenderer(options)
  }
}