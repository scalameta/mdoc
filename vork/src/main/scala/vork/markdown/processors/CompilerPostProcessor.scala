package vork.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence}
import org.langmeta.inputs.Input
import vork.markdown.processors.MarkdownCompiler.SectionInput
import vork.{Markdown, Options, Processor}

class CompilerPostProcessor(options: Options, compiler: MarkdownCompiler) extends DocumentPostProcessor {

  object VorkCodeFence {
    def unapply(block: FencedCodeBlock): Option[(FencedCodeBlock, FencedCodeMod)] =
      block.getInfo.toString match {
        case FencedCodeMod(mod) => Some(block -> mod)
        case _ => None
      }
  }
  override def processDocument(doc: Document): Document = {
    import vork.Markdown._
    import scala.collection.JavaConverters._
    val originPath = doc
      .get(Processor.PathKey)
      .getOrElse(sys.error("Path key does not exist in Flexmark's settings!!"))
      .toString

    val fences = collect[FencedCodeBlock, (FencedCodeBlock, FencedCodeMod)](doc) {
      case VorkCodeFence(block, mod) => block -> mod
    }
    if (fences.nonEmpty) {
      val code = fences.map {
        case (block, mod) =>
          val input = Input.VirtualFile(originPath, block.getContentChars.toString)
          import scala.meta._
          val source = dialects.Sbt1(input).parse[Source].get
          SectionInput(source, mod)
      }
      val rendered = MarkdownCompiler.renderInputs(code, compiler)
      rendered.sections.zip(fences).foreach {
        case (section, (block, mod)) =>
          block.setInfo(CharSubSequence.of("scala"))
          mod match {
            case FencedCodeMod.Fail =>
              val str = MarkdownCompiler.renderEvaluatedSection(section)
              val content: BasedSequence = CharSubSequence.of(str)
              block.setContent(List(content).asJava)
            case FencedCodeMod.Default =>
              val str = MarkdownCompiler.renderEvaluatedSection(section)
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
  class Factory(options: Options, compiler: MarkdownCompiler) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new CompilerPostProcessor(options, compiler)
    }
  }
}
