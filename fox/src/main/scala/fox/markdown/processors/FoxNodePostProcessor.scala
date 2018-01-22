package fox.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Heading, Node}
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.parser.block.{NodePostProcessor, NodePostProcessorFactory}
import com.vladsch.flexmark.util.NodeTracker
import fox.markdown.FoxHelpers

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
    override def create(document: ast.Document): NodePostProcessor = {
      new FoxNodePostProcessor
    }
  }
}