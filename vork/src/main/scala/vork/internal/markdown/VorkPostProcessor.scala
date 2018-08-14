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
import vork.internal.cli.Context
import vork.internal.cli.MainOps
import vork.internal.markdown.MarkdownCompiler.SectionInput
import vork.internal.markdown.VorkModifier._

class VorkPostProcessor(implicit context: Context) extends DocumentPostProcessor {
  import context._

  class VorkCodeFence(input: Input) {
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
              val pos = Position.Range(input, start, end)
              context.logger.error(pos, msg)
              None
            }
        }
      }
    }
    def unapply(block: FencedCodeBlock): Option[(FencedCodeBlock, VorkModifier)] = {
      getModifier(block) match {
        case Some(mod) =>
          Some(block -> mod)
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

    val fences = Markdown.collect[FencedCodeBlock, (FencedCodeBlock, VorkModifier)](doc) {
      case VorkCodeFence(block, mod) =>
        block -> mod
    }
    val custom = fences.collect {
      case (f, c: Custom) => (f, c)
    }
    custom.foreach {
      case (block, Custom(mod, info)) =>
        val oldText = block.getContentChars().toString
        val newText = mod.process(info, oldText)
        replace(doc, block, newText)
    }
    val toCompile = fences.collect {
      case (f, c) if !c.isCustom => (f, c)
    }
    if (toCompile.nonEmpty) {
      val code = toCompile.map {
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
      rendered.sections.zip(toCompile).foreach {
        case (section, (block, mod)) =>
          block.setInfo(CharSubSequence.of("scala"))
          mod match {
            case VorkModifier.Default | VorkModifier.Fail =>
              val str = MarkdownCompiler.renderEvaluatedSection(rendered, section, logger)
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
