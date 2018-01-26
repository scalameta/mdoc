package fox.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence}
import fox.Markdown
import fox.Options
import fox.markdown.repl.{AmmoniteRepl, Evaluator}

class AmmonitePostProcessor(options: Options) extends DocumentPostProcessor {
  private val repl = new AmmoniteRepl()
  repl.loadClasspath(options.classpath)
  override def processDocument(doc: Document): Document = {
    import fox.Markdown._
    import scala.collection.JavaConverters._
    traverse[FencedCodeBlock](doc) {
      case block =>
        block.getInfo.toString match {
          case FencedCodeMod(mod) =>
            block.setInfo(CharSubSequence.of("scala"))
            val code = block.getContentChars().toString
            val evaluated = Evaluator.evaluate(repl, code, mod)
            mod match {
              case FencedCodeMod.Default | FencedCodeMod.Fail =>
                val content: BasedSequence = CharSubSequence.of(evaluated)
                block.setContent(List(content).asJava)
              case FencedCodeMod.Passthrough =>
                // TODO(olafur) avoid creating fresh MutableDataSet for every passthrough
                // We may be initializing a new ammonite repl every time here, which may be slow.
                val child = Markdown.parse(evaluated, options)
                block.insertAfter(child)
                block.unlink()
            }
          case _ =>
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
