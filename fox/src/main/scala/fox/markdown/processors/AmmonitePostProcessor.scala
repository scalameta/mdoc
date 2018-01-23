package fox.markdown.processors

import java.io.{FileInputStream, FileOutputStream, PipedInputStream, PipedOutputStream}
import java.nio.file.Files

import com.vladsch.flexmark.ast
import com.vladsch.flexmark.ast.{BlockContent, Document, FencedCodeBlock}
import com.vladsch.flexmark.parser.block.{DocumentPostProcessor, DocumentPostProcessorFactory}
import com.vladsch.flexmark.util.sequence.{BasedSequence, PrefixedSubSequence}
import fastparse.core.Parsed
import fox.Options
import fox.markdown.FoxHelpers

import scala.util.{Failure, Success, Try}

class AmmonitePostProcessor(options: Options) extends DocumentPostProcessor {
  import ammonite.util.Res
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

  private val repl = new AmmoniteRepl
  import FoxHelpers.stringToCharSequence
  override def processDocument(doc: Document): Document = {
    import fox.Markdown._
    import scala.collection.JavaConverters._
    traverse[FencedCodeBlock](doc) {
      case block =>
        val prefix = block.getInfo()
        if (prefix.startsWith("scala")) {
          val code = block.getContentChars().toString()

          val b = new StringBuilder()
          for { stmt <- parseCode(code) } {
            val result = repl.run(stmt, repl.currentLine)
            val replOutput = result._3
            b.append("@ ").append(stmt).append(replOutput).append("\n")
            println(result)
          }

          val ammoniteOut: BasedSequence = b.toString()
          block.setContent(List(ammoniteOut).asJava)
        }
    }
    doc
  }
}

object AmmonitePostProcessor {
  class Factory(options: Options) extends DocumentPostProcessorFactory {
    override def create(document: ast.Document): DocumentPostProcessor = {
      new AmmonitePostProcessor(options)
    }
  }
}
