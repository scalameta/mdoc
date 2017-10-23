package fox.markdown

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.NodeTracker
import com.vladsch.flexmark.util.options.MutableDataHolder
import fox.markdown

class HeaderIdExtension extends Parser.ParserExtension {
  override def extend(parserBuilder: Parser.Builder): Unit = {
    parserBuilder.postProcessorFactory(
      new markdown.HeaderIdPostProcessor.Factory
    )
  }
  override def parserOptions(options: MutableDataHolder): Unit = ()
}

object HeaderIdExtension {
  def create(): Extension = new HeaderIdExtension
}

class HeaderIdPostProcessor extends NodePostProcessor {
  override def process(state: NodeTracker, node: Node): Unit = node match {
    case h: Heading =>
      pprint.log(state)
      pprint.log(node)
    case _ =>
  }
}
object HeaderIdPostProcessor {
  class Factory extends NodePostProcessorFactory(false) {
    addNodes(classOf[Heading])
    override def create(document: Document): NodePostProcessor = {
      new HeaderIdPostProcessor
    }
  }
}
