package vork.internal.markdown

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.parser.block.DocumentPostProcessor
import com.vladsch.flexmark.parser.block.DocumentPostProcessorFactory
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.CharSubSequence
import scala.meta.inputs.Input
import vork.internal.cli.Context
import vork.internal.cli.MainOps
import vork.internal.markdown.MarkdownCompiler.SectionInput

class VorkPostProcessor(implicit context: Context) extends DocumentPostProcessor {
  import context._

  object VorkCodeFence {
    def unapply(block: FencedCodeBlock): Option[(FencedCodeBlock, VorkModifier)] = {
      block.getInfo.toString match {
        case VorkModifier(mod) =>
          Some(block -> mod)
        case _ => None
      }
    }
  }
  override def processDocument(doc: Document): Document = {
    import Markdown._
    import scala.collection.JavaConverters._
    val baseInput = doc
      .get(MainOps.InputKey)
      .getOrElse(sys.error(s"Missing DataKey ${MainOps.InputKey}"))
    val filename = baseInput.path

    val fences = collect[FencedCodeBlock, (FencedCodeBlock, VorkModifier)](doc) {
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
            case VorkModifier.Default | VorkModifier.Fail =>
              val str = MarkdownCompiler.renderEvaluatedSection(section, logger)
              val content: BasedSequence = CharSubSequence.of(str)
              block.setContent(List(content).asJava)
            case VorkModifier.Passthrough =>
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

object VorkPostProcessor {
  class Factory(context: Context) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new VorkPostProcessor()(context)
    }
  }
}
