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
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import vork.CustomModifier
import vork.internal.cli.Context
import vork.internal.cli.MainOps
import vork.internal.document.VorkExceptions
import vork.internal.markdown.MarkdownCompiler.SectionInput
import vork.internal.markdown.VorkModifier._

final class CustomModifierException(mod: CustomModifier, cause: Throwable)
    extends Exception(mod.name, cause)
    with NoStackTrace

class VorkPostProcessor(implicit context: Context) extends DocumentPostProcessor {
  import context._

  case class CustomBlockInput(block: FencedCodeBlock, input: Input, mod: Custom)
  case class BlockInput(block: FencedCodeBlock, input: Input, mod: VorkModifier)

  class VorkCodeFence(baseInput: Input) {
    def getModifier(block: FencedCodeBlock): Option[VorkModifier] = {
      val string = block.getInfo.toString
      if (!string.startsWith("scala vork")) None
      else {
        if (!string.contains(':')) Some(Default)
        else {
          val mode = string.stripPrefix("scala vork:")
          VorkModifier(mode)
            .orElse {
              val (name, info) = mode.split(":", 2) match {
                case Array(a) => (a, "")
                case Array(a, b) => (a, b)
              }
              context.settings.modifiers.collectFirst {
                case mod if mod.name == name =>
                  Custom(mod, info)
              }
            }
            .orElse {
              val expected = VorkModifier.all.map(_.toString.toLowerCase()).mkString(", ")
              val msg = s"Invalid mode '$mode'. Expected one of: $expected"
              val offset = "scala vork:".length
              val start = block.getInfo.getStartOffset + offset
              val end = block.getInfo.getEndOffset
              val pos = Position.Range(baseInput, start, end)
              context.reporter.error(pos, msg)
              None
            }
        }
      }
    }
    def unapply(block: FencedCodeBlock): Option[BlockInput] = {
      getModifier(block) match {
        case Some(mod) =>
          val child = block.getFirstChild
          val start = child.getStartOffset
          val end = child.getEndOffset
          val input = Input.Slice(baseInput, start, end)
          Some(BlockInput(block, input, mod))
        case _ => None
      }
    }
  }

  override def processDocument(doc: Document): Document = {
    import scala.collection.JavaConverters._
    val baseInput = doc
      .get(MainOps.InputKey)
      .getOrElse(sys.error(s"Missing DataKey ${MainOps.InputKey}"))
    val filename = baseInput.path
    val VorkCodeFence = new VorkCodeFence(baseInput)

    val fences = Markdown.collect[FencedCodeBlock, BlockInput](doc) {
      case VorkCodeFence(input) => input
    }
    val custom = fences.collect {
      case BlockInput(a, b, c: Custom) => CustomBlockInput(a, b, c)
    }
    custom.foreach {
      case CustomBlockInput(block, input, Custom(mod, info)) =>
        try {
          val newText = mod.process(info, input, reporter)
          replace(doc, block, newText)
        } catch {
          case NonFatal(e) =>
            val length = math.max(0, input.chars.length - 1)
            val pos = Position.Range(input, 0, length)
            VorkExceptions.trimStacktrace(e)
            val exception = new CustomModifierException(mod, e)
            reporter.error(pos, exception)
        }
    }
    val toCompile = fences.collect {
      case i if !i.mod.isCustom => i
    }
    if (toCompile.nonEmpty) {
      val code = toCompile.map {
        case BlockInput(block, input, mod) =>
          import scala.meta._
          val source = dialects.Sbt1(input).parse[Source].get
          SectionInput(input, source, mod)
      }
      val rendered = MarkdownCompiler.renderInputs(code, compiler, reporter, filename)
      rendered.sections.zip(toCompile).foreach {
        case (section, BlockInput(block, _, mod)) =>
          block.setInfo(CharSubSequence.of("scala"))
          mod match {
            case VorkModifier.Default | VorkModifier.Fail =>
              val str = MarkdownCompiler.renderEvaluatedSection(rendered, section, reporter)
              val content: BasedSequence = CharSubSequence.of(str)
              block.setContent(List(content).asJava)
            case VorkModifier.Passthrough =>
              replace(doc, block, section.out)
            case c: VorkModifier.Custom =>
              throw new IllegalArgumentException(c.toString)
          }
      }
    }
    doc
  }

  def replace(doc: Document, node: Node, text: String): Unit = {
    val markdownOptions = new MutableDataSet()
    markdownOptions.setAll(doc)
    val child = Markdown.parse(CharSubSequence.of(text), markdownOptions)
    node.insertAfter(child)
    node.unlink()
  }
}

object VorkPostProcessor {
  class Factory(context: Context) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new VorkPostProcessor()(context)
    }
  }
}
