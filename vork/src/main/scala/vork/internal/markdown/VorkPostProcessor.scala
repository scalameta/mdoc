package vork.internal.markdown

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.parser.block.DocumentPostProcessor
import com.vladsch.flexmark.parser.block.DocumentPostProcessorFactory
import com.vladsch.flexmark.util.options.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.CharSubSequence
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.util.control.NonFatal
import scala.collection.JavaConverters._
import vork.internal.cli.Context
import vork.internal.cli.MainOps
import vork.internal.document.VorkExceptions
import vork.internal.markdown.MarkdownCompiler.SectionInput
import vork.internal.markdown.VorkModifier._

class VorkPostProcessor(implicit ctx: Context) extends DocumentPostProcessor {

  override def processDocument(doc: Document): Document = {
    import scala.collection.JavaConverters._
    val docInput = doc
      .get(MainOps.InputKey)
      .getOrElse(sys.error(s"Missing DataKey ${MainOps.InputKey}"))
    val (scalaInputs, customInputs) = collectBlockInputs(doc, docInput)
    customInputs.foreach { block =>
      processCustomInput(doc, block)
    }
    if (scalaInputs.nonEmpty) {
      processScalaInputs(doc, scalaInputs, docInput.path)
    }
    doc
  }

  def processCustomInput(doc: Document, custom: CustomBlockInput): Unit = {
    val CustomBlockInput(block, input, Custom(mod, info)) = custom
    try {
      val newText = mod.process(info, input, ctx.reporter)
      replaceNodeWithText(doc, block, newText)
    } catch {
      case NonFatal(e) =>
        val pos = Position.Range(input, 0, input.chars.length)
        VorkExceptions.trimStacktrace(e)
        val exception = new CustomModifierException(mod, e)
        ctx.reporter.error(pos, exception)
    }
  }

  def processScalaInputs(
      doc: Document,
      inputs: List[ScalaBlockInput],
      filename: String
  ): Unit = {
    val code = inputs.map {
      case ScalaBlockInput(_, input, mod) =>
        import scala.meta._
        dialects.Sbt1(input).parse[Source] match {
          case parsers.Parsed.Success(source) =>
            SectionInput(input, source, mod)
          case parsers.Parsed.Error(pos, msg, _) =>
            ctx.reporter.error(pos, msg)
            SectionInput(input, Source(Nil), mod)
        }
    }
    val rendered = MarkdownCompiler.renderInputs(code, ctx.compiler, ctx.reporter, filename)
    rendered.sections.zip(inputs).foreach {
      case (section, ScalaBlockInput(block, _, mod)) =>
        block.setInfo(CharSubSequence.of("scala"))
        mod match {
          case VorkModifier.Default | VorkModifier.Fail =>
            val str = MarkdownCompiler.renderEvaluatedSection(rendered, section, ctx.reporter)
            val content: BasedSequence = CharSubSequence.of(str)
            block.setContent(List(content).asJava)
          case VorkModifier.Passthrough =>
            replaceNodeWithText(doc, block, section.out)
          case c: VorkModifier.Custom =>
            throw new IllegalArgumentException(c.toString)
        }
    }
  }

  def replaceNodeWithText(enclosingDoc: Document, toReplace: Node, text: String): Unit = {
    val markdownOptions = new MutableDataSet()
    markdownOptions.setAll(enclosingDoc)
    val child = Markdown.parse(CharSubSequence.of(text), markdownOptions)
    toReplace.insertAfter(child)
    toReplace.unlink()
  }

  def collectBlockInputs(
      doc: Document,
      docInput: Input.VirtualFile
  ): (List[ScalaBlockInput], List[CustomBlockInput]) = {
    val InterestingCodeFence = new BlockCollector(ctx, docInput)
    val inputs = List.newBuilder[ScalaBlockInput]
    val customs = List.newBuilder[CustomBlockInput]
    Markdown.traverse[FencedCodeBlock](doc) {
      case InterestingCodeFence(input) =>
        input.mod match {
          case custom: Custom =>
            customs += CustomBlockInput(input.block, input.input, custom)
          case _ =>
            inputs += input
        }
    }
    (inputs.result(), customs.result())
  }
}

object VorkPostProcessor {
  class Factory(context: Context) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new VorkPostProcessor()(context)
    }
  }
}
