package fox.markdown

import fox.Options

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.html.renderer.{AttributablePart, HeaderIdGenerator, NodeRendererContext}
import com.vladsch.flexmark.util.html.Attributes
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.html.{
  AttributeProvider,
  HtmlRenderer,
  IndependentAttributeProviderFactory
}

object FoxAttributeProvider {
  class Factory extends IndependentAttributeProviderFactory {
    override def create(context: NodeRendererContext): AttributeProvider =
      new FoxAttributeProvider
  }
}

class FoxAttributeProvider extends AttributeProvider {
  override def setAttributes(
      node: ast.Node,
      part: AttributablePart,
      attributes: Attributes
  ): Unit = node match {
//    case p: ast.FencedCodeBlock =>
//      val old = Option(attributes.get("class")).fold("")(_.getValue)
//      attributes.replaceValue("class", "prettyprint prettyprinted " + old)
    case l: ast.Link =>
      if (l.getParent.isInstanceOf[ast.Heading]) {
        attributes.replaceValue("class", "headerlink")
      }
    case h: ast.Heading =>
      attributes.replaceValue(
        "id",
        HeaderIdGenerator.generateId(h.getText, null, false)
      )
    case _ =>
  }
}

class FoxAttributeProviderExtension(options: Options) extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = ()
  override def extend(
      rendererBuilder: HtmlRenderer.Builder,
      rendererType: String
  ): Unit = {
    rendererBuilder.nodeRendererFactory(new FoxNodeRenderer.Factory(options))
    rendererBuilder.attributeProviderFactory(
      new FoxAttributeProvider.Factory
    )
  }
}
object FoxAttributeProviderExtension {
  def create(options: Options) = new FoxAttributeProviderExtension(options)
}
