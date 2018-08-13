package vork.markdown

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ast
import com.vladsch.flexmark.parser.LinkRefProcessor
import com.vladsch.flexmark.parser.LinkRefProcessorFactory
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.PrefixedSubSequence
import vork.Context

class VorkParserExtension(context: Context) extends Parser.ParserExtension {
  class SiteVariableInjector(site: Map[String, String], document: ast.Document)
      extends LinkRefProcessor {

    /**
      * Creates a text node with the value of a site variable.
      *
      * Using `PrefixedSubSequence` and then removing suffix is the only way I found to simulate
      * `replace`. Converting from `String` to `BasedSequence` via `stringToCharSequence` does not
      * work because Flexmark's `Text` only uses the start and end offsets and does not do an
      * in-place replacement of the contents of the returned sequence. These start and end offsets
      * select from the original text, not the modified value that we wanted to insert.
      * Creating a custom node extending `ContentNode` or `CustomNode` did not help either.
      */
    override def createNode(nodeChars: BasedSequence): ast.Node = {
      nodeChars.toString match {
        case VariableInjectionPattern(key) =>
          val value = site.getOrElse(key, sys.error(s"Missing '$key' site variable."))
          new ast.Text(PrefixedSubSequence.of(value, nodeChars).removeSuffix(nodeChars))
        case _ =>
          sys.error("Flexmark matched a variable injection which is not of the expected shape.")
      }
    }

    private final val totalLength = document.getTextLength
    private final val VariableInjectionPattern = "!\\[([^\\]\\[]*)\\]".r
    override def isMatch(nodeChars: BasedSequence): Boolean = {
      val matches = nodeChars.toString.matches(VariableInjectionPattern.regex)
      val startNext = nodeChars.getEndOffset
      val endNext = startNext + 1
      // As bracket nesting level is 0, we need to check that the following char does not start with `[`
      matches && (
        startNext == totalLength ||
        !document.getChars.baseSubSequence(startNext, endNext).startsWith("[")
      )
    }

    override def adjustInlineText(doc: ast.Document, node: ast.Node): BasedSequence = node.getChars
    override def getBracketNestingLevel: Int = 0
    override def getWantExclamationPrefix: Boolean = true
    override def updateNodeElements(document: ast.Document, node: ast.Node): Unit = ()
    override def allowDelimiters(chars: BasedSequence, doc: ast.Document, node: ast.Node): Boolean =
      true
  }

  class SiteVariableInjectorFactory extends LinkRefProcessorFactory {
    override def getBracketNestingLevel(options: DataHolder): Int = 0
    override def getWantExclamationPrefix(options: DataHolder): Boolean = true
    override def create(document: ast.Document): LinkRefProcessor =
      new SiteVariableInjector(context.args.vars, document)
  }

  override def extend(parserBuilder: Parser.Builder): Unit = {
    parserBuilder.linkRefProcessorFactory(new SiteVariableInjectorFactory)
    parserBuilder.postProcessorFactory(
      new processors.CompilerPostProcessor.Factory(context)
    )
  }
  override def parserOptions(options: MutableDataHolder): Unit = ()
}

object VorkParserExtension {
  def create(context: Context): Extension = {
    new VorkParserExtension(context)
  }
}
