package vork.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence}
import java.nio.file.Paths
import scala.meta.inputs.Input
import vork.Context
import vork.markdown.processors.MarkdownCompiler.SectionInput
import vork.{Args, Markdown, Processor}

class CompilerPostProcessor(implicit context: Context) extends DocumentPostProcessor {
  import context._

  object VorkCodeFence {
    def unapply(block: FencedCodeBlock): Option[(FencedCodeBlock, FencedCodeMod)] = {
      block.getInfo.toString match {
        case FencedCodeMod(mod) =>
          Some(block -> mod)
        case _ => None
      }
    }
  }
  override def processDocument(doc: Document): Document = {
    import vork.Markdown._
    import scala.collection.JavaConverters._
    val baseInput = doc
      .get(Processor.InputKey)
      .getOrElse(sys.error("INput key does not exist in Flexmark's settings!!"))
    val filename = baseInput.path

    val fences = collect[FencedCodeBlock, (FencedCodeBlock, FencedCodeMod)](doc) {
      case VorkCodeFence(block, mod) =>
        block -> mod
    }
    if (fences.nonEmpty) {
      val code = fences.map {
        case (block, mod) =>
          val child = block.getFirstChild
          val start = child.getStartOffset
          val end = child.getEndOffset
          val input = Input.Slice(baseInput, start, end)
          import scala.meta._
          val source = dialects.Sbt1(input).parse[Source].get
          SectionInput(input, source, mod)
      }
      val rendered = MarkdownCompiler.renderInputs(code, compiler, logger, filename)
      rendered.sections.zip(fences).foreach {
        case (section, (block, mod)) =>
          block.setInfo(CharSubSequence.of("scala"))
          mod match {
            case FencedCodeMod.Default | FencedCodeMod.Fail =>
              val str = MarkdownCompiler.renderEvaluatedSection(section, logger)
              val content: BasedSequence = CharSubSequence.of(str)
              block.setContent(List(content).asJava)
            case FencedCodeMod.Passthrough =>
              val markdownOptions = new MutableDataSet()
              markdownOptions.setAll(doc)
              val child = Markdown.parse(CharSubSequence.of(section.out), markdownOptions)
              block.insertAfter(child)
              block.unlink()
          }
      }
    }
    doc
  }
}

object CompilerPostProcessor {
  class Factory(context: Context) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new CompilerPostProcessor()(context)
    }
  }
}
