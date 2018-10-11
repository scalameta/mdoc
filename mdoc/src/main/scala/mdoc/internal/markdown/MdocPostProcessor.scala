package mdoc.internal.markdown

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
import mdoc.internal.cli.Context
import mdoc.internal.document.MdocExceptions
import mdoc.internal.markdown.Modifier._
import mdoc.internal.pos.PositionSyntax._

class MdocPostProcessor(implicit ctx: Context) extends DocumentPostProcessor {

  override def processDocument(doc: Document): Document = {
    import scala.collection.JavaConverters._
    val docInput = doc
      .get(Markdown.InputKey)
      .getOrElse(sys.error(s"Missing DataKey ${Markdown.InputKey}"))
    val (scalaInputs, customInputs) = collectBlockInputs(doc, docInput)
    customInputs.foreach { block =>
      processStringInput(doc, block)
    }
    if (scalaInputs.nonEmpty) {
      processScalaInputs(doc, scalaInputs, docInput.toFilename(ctx.settings))
    }
    doc
  }

  def processStringInput(doc: Document, custom: StringBlockInput): Unit = {
    val StringBlockInput(block, input, Str(mod, info)) = custom
    try {
      val newText = mod.process(info, input, ctx.reporter)
      replaceNodeWithText(doc, block, newText)
    } catch {
      case NonFatal(e) =>
        val pos = Position.Range(input, 0, input.chars.length)
        MdocExceptions.trimStacktrace(e)
        val exception = new StringModifierException(mod, e)
        ctx.reporter.error(pos, exception)
    }
  }

  def processScalaInputs(doc: Document, inputs: List[ScalaBlockInput], filename: String): Unit = {
    val sectionInputs = inputs.map {
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
    val instrumented = Instrumenter.instrument(sectionInputs)
    if (ctx.settings.verbose) {
      ctx.reporter.info(s"Instrumented $filename")
      ctx.reporter.println(instrumented)
    }
    val rendered = MarkdownCompiler.buildDocument(
      ctx.compiler,
      ctx.reporter,
      sectionInputs,
      instrumented,
      filename
    )
    rendered.sections.zip(inputs).foreach {
      case (section, ScalaBlockInput(block, _, mod)) =>
        block.setInfo(CharSubSequence.of("scala"))
        mod match {
          case Modifier.Silent =>
          case Modifier.Default | Modifier.Fail =>
            val str = Renderer.renderEvaluatedSection(
              rendered,
              section,
              ctx.reporter,
              ctx.settings.variablePrinter
            )
            val content: BasedSequence = CharSubSequence.of(str)
            block.setContent(List(content).asJava)
          case Modifier.Passthrough =>
            replaceNodeWithText(doc, block, section.out)
          case Modifier.Crash =>
            val stacktrace =
              Renderer.renderCrashSection(section, ctx.reporter, rendered.edit)
            replaceNodeWithText(doc, block, stacktrace)
          case c: Modifier.Str =>
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
      docInput: Input
  ): (List[ScalaBlockInput], List[StringBlockInput]) = {
    val InterestingCodeFence = new BlockInput(ctx, docInput)
    val inputs = List.newBuilder[ScalaBlockInput]
    val strings = List.newBuilder[StringBlockInput]
    Markdown.traverse[FencedCodeBlock](doc) {
      case InterestingCodeFence(input) =>
        input.mod match {
          case string: Str =>
            strings += StringBlockInput(input.block, input.input, string)
          case _ =>
            inputs += input
        }
    }
    (inputs.result(), strings.result())
  }
}

object MdocPostProcessor {
  class Factory(context: Context) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new MdocPostProcessor()(context)
    }
  }
}
