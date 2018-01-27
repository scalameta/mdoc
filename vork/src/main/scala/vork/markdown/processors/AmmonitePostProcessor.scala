package vork.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence}
import vork.{Markdown, Options, Processor}
import vork.markdown.repl.{AmmoniteRepl, RichCodeBlock, Evaluator, Position}

class AmmonitePostProcessor(options: Options) extends DocumentPostProcessor {
  private val repl = new AmmoniteRepl()
  repl.loadClasspath(options.classpath)
  override def processDocument(doc: Document): Document = {
    import vork.Markdown._
    import scala.collection.JavaConverters._
    val originPath = doc
      .get(Processor.PathKey)
      .getOrElse(sys.error("Path key does not exist in Flexmark's settings!!"))
    var errors = List.empty[String]
    traverse[FencedCodeBlock](doc) {
      case block =>
        block.getInfo.toString match {
          case FencedCodeMod(mod) =>
            block.setInfo(CharSubSequence.of("scala"))
            val code = block.getContentChars().toString
            val pos = Position(originPath, block.getStartLineNumber, block.getEndLineNumber)
            Evaluator.evaluate(repl, RichCodeBlock(code, pos), mod) match {
              case Left(error) => errors ::= error
              case Right(str) =>
                mod match {
                  case FencedCodeMod.Default | FencedCodeMod.Fail =>
                    val content: BasedSequence = CharSubSequence.of(str)
                    block.setContent(List(content).asJava)
                  case FencedCodeMod.Passthrough =>
                    val markdownOptions = new MutableDataSet()
                    markdownOptions.setAll(doc) // A doc is a mutable data set itself
                    val child = Markdown.parse(CharSubSequence.of(str), markdownOptions)
                    block.insertAfter(child)
                    block.unlink()
                }
            }
          case _ => ()
        }
    }

    if (errors.nonEmpty) { // Report errors in batch
      throw Evaluator.CodeFenceFailure(errors.reverse)
    } else doc
  }
}

object AmmonitePostProcessor {
  class Factory(options: Options) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new AmmonitePostProcessor(options)
    }
  }
}
