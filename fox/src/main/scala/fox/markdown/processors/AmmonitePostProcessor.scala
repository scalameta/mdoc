package fox.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{BlockContent, Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.sequence.{BasedSequence, PrefixedSubSequence}
import fox.Options
import fox.markdown.FoxHelpers

class AmmonitePostProcessor(options: Options) extends DocumentPostProcessor {
  import FoxHelpers.stringToCharSequence
  override def processDocument(doc: Document): Document = {
    import fox.Markdown._
    import scala.collection.JavaConverters._
    traverse[FencedCodeBlock](doc) { case block =>
      val prefix = block.getInfo()
      if (prefix.startsWith("scala")) {
        val nodeChars = block.getChars()
        val newChars = PrefixedSubSequence.of("HEY MAN", nodeChars).removeSuffix(nodeChars)
        val bs: BasedSequence = "alksjdf;lkajsflkj"
        val newLines = List(bs).asJava
        block.setContent(newLines)
      }
    }
    doc
  }
}

object AmmonitePostProcessor {
  class Factory(options: Options) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new AmmonitePostProcessor(options)
    }
  }
}
