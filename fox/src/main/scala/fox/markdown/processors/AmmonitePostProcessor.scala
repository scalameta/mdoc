package fox.markdown.processors

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence}
import fastparse.core.Parsed
import fox.Markdown
import fox.Options
import metaconfig.ConfError

class AmmonitePostProcessor(options: Options) extends DocumentPostProcessor {
  import ammonite.interp.Parsers
  def parseCode(code: String): Seq[String] = {
    Parsers.Splitter.parse(code) match {
      case Parsed.Success(value, idx) => value
      case Parsed.Failure(_, index, extra) =>
        sys.error(
          fastparse.core.ParseError.msg(extra.input, extra.traced.expected, index)
        )
    }
  }

  private val repl = new AmmoniteRepl()
  repl.loadClasspath(options.classpath)
  override def processDocument(doc: Document): Document = {
    import fox.Markdown._
    import scala.collection.JavaConverters._
    traverse[FencedCodeBlock](doc) {
      case block =>
        block.getInfo.toString match {
          case Mod(mod) =>
            block.setInfo(CharSubSequence.of("scala"))
            val code = block.getContentChars().toString
            val b = new StringBuilder()
            for { stmt <- parseCode(code) } {
              val result = repl.run(stmt, repl.currentLine)
              (result.evaluated.isSuccess, mod) match {
                case (true, Mod.Fail) =>
                  throw CodeFenceSuccess(s"expected failure, got success for code block:\n$code")
                case (true, _) => // Ok, do nothing
                case (false, Mod.Fail) => // Ok, do nothing
                case (false, _) =>
                  throw CodeFenceError(
                    s"expected success, got failure ${pprint.apply(result)} for code block:\n$code"
                  )
              }
              mod match {
                case Mod.Default | Mod.Fail =>
                  b.append("@ ")
                    .append(stmt)
                    .append(
                      if (!result.error.isEmpty) result.error
                      else result.outString
                    )
                    .append("\n")
                case Mod.Passthrough =>
                  b.append(result.stdout)
              }
            }
            if (b.charAt(b.size - 1) == '\n') {
              b.setLength(b.length - 1) // strip trailing newline
            }
            val ammoniteOut = b.toString()
            mod match {
              case Mod.Default | Mod.Fail =>
                val content: BasedSequence = CharSubSequence.of(ammoniteOut)
                block.setContent(List(content).asJava)
              case Mod.Passthrough =>
                // TODO(olafur) avoid creating fresh MutableDataSet for every passthrough
                // We may be initializing a new ammonite repl every time here, which may be slow.
                val child = Markdown.parse(ammoniteOut, options)
                block.insertAfter(child)
                block.unlink()
            }
          case _ =>
        }
    }
    doc
  }
}

case class CodeFenceError(msg: String) extends Exception(msg)
case class CodeFenceSuccess(msg: String) extends Exception(msg)

object AmmonitePostProcessor {
  class Factory(options: Options) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new AmmonitePostProcessor(options)
    }
  }
}
