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
import java.util
import mdoc.PostModifierContext
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.util.control.NonFatal
import scala.collection.JavaConverters._
import mdoc.internal.cli.Context
import mdoc.internal.document.MdocExceptions
import mdoc.internal.markdown.Modifier._
import mdoc.internal.pos.PositionSyntax._
import pprint.TPrintColors

class MdocPostProcessor(implicit ctx: Context) extends DocumentPostProcessor {

  override def processDocument(doc: Document): Document = {
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
            ctx.reporter.error(pos.toUnslicedPosition, msg)
            SectionInput(input, Source(Nil), mod)
        }
    }
    val instrumented = Instrumenter.instrument(sectionInputs)
    if (ctx.settings.verbose) {
      ctx.reporter.info(s"Instrumented $filename")
      ctx.reporter.println(instrumented)
    }
    val inputFile =
      doc
        .get(Markdown.RelativePathKey)
        .getOrElse(throw new NoSuchElementException(s"InputFile: $filename"))
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
        def defaultRender: String = Renderer.renderEvaluatedSection(
          rendered,
          section,
          ctx.reporter,
          ctx.settings.variablePrinter,
          ctx.compiler
        )
        mod match {
          case Modifier.Post(modifier, info) =>
            val variables = for {
              (stat, i) <- section.section.statements.zipWithIndex
              n = section.section.statements.length
              (binder, j) <- stat.binders.zipWithIndex
              m = stat.binders.length
            } yield {
              new mdoc.Variable(
                binder.name,
                binder.tpe.render(TPrintColors.BlackWhite),
                binder.value,
                binder.pos.toMeta(section),
                j,
                n,
                i,
                m
              )
            }
            val postCtx = new PostModifierContext(
              info,
              section.input,
              defaultRender,
              variables,
              ctx.reporter,
              inputFile,
              ctx.settings
            )
            val postRender = modifier.process(postCtx)
            replaceNodeWithText(doc, block, postRender)
          case m: Modifier.Builtin =>
            if (m.isPassthrough) {
              replaceNodeWithText(doc, block, section.out)
            } else if (m.isInvisible) {
              replaceNodeWithText(doc, block, "")
            } else if (m.isCrash) {
              val stacktrace =
                Renderer.renderCrashSection(section, ctx.reporter, rendered.edit)
              replaceNodeWithText(doc, block, stacktrace)
            } else if (m.isSilent) {
              () // Do nothing
            } else if (m.isDefault || m.isReset || m.isFail) {
              block.setContent(List[BasedSequence](CharSubSequence.of(defaultRender)).asJava)
            } else {
              throw new IllegalArgumentException(m.toString)
            }
          case c: Modifier.Str =>
            throw new IllegalArgumentException(c.toString)
        }
    }
  }

  def asBasedSequence(string: String): util.List[BasedSequence] = {
    List[BasedSequence](CharSubSequence.of(string)).asJava
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
