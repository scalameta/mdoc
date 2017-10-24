package fox.markdown

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.html.AttributeProvider
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory
import com.vladsch.flexmark.html.renderer.AttributablePart
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.NodeTracker
import com.vladsch.flexmark.util.html.Attributes
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.CharSubSequence
import fox.markdown

class FoxParserExtension extends Parser.ParserExtension {
  override def extend(parserBuilder: Parser.Builder): Unit = {
    parserBuilder.postProcessorFactory(
      new markdown.FoxNodePostProcessor.Factory
    )
  }
  override def parserOptions(options: MutableDataHolder): Unit = ()
}

object FoxParserExtension {
  def create(): Extension = new FoxParserExtension
}

class FoxNodePostProcessor extends NodePostProcessor {
  implicit def stringToCharSequence(string: String): BasedSequence = {
    CharSubSequence.of(string)
  }

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
class FoxAttributeProviderExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder) = ()
  override def extend(
      rendererBuilder: HtmlRenderer.Builder,
      rendererType: String
  ) = {
    rendererBuilder.attributeProviderFactory(
      new FoxAttributeProvider.Factory
    )
  }
}
object FoxAttributeProviderExtension {
  def create() = new FoxAttributeProviderExtension
}
