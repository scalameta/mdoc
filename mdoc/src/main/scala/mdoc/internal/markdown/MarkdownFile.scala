package mdoc.internal.markdown

import scala.meta.inputs.Position
import scala.meta.inputs.Input
import mdoc.Reporter

import scala.collection.mutable
import scala.meta.io.RelativePath
import mdoc.internal.cli.{InputFile, Settings}

final case class MarkdownFile(input: Input, file: InputFile, parts: List[MarkdownPart]) {
  private val appends = mutable.ListBuffer.empty[String]
  def appendText(text: String): Unit = {
    appends += text
  }
  def renderToString: String = {
    val out = new StringBuilder()
    parts.foreach(_.renderToString(out))
    appends.foreach(a => out.append(a).append("\n"))
    out.toString()
  }
}
object MarkdownFile {
  object syntax {
    private[markdown] implicit class StringOps(private val x: String) extends AnyVal {
      def isNL: Boolean = x.forall { c => c == '\n' || c == '\r' }
    }

    private[markdown] implicit class StringBuilderOps(private val x: StringBuilder) extends AnyVal {
      def appendLinesPrefixed(prefix: String, text: String): Unit = {
        text.linesWithSeparators foreach { line =>
          if (line.nonEmpty && !line.isNL && !line.startsWith(prefix)) x.append(prefix)
          x.append(line)
        }
      }
    }
  }
  sealed abstract class State
  object State {
    case class CodeFence(start: Int, backticks: String, info: String, indent: Int) extends State
    case object Text extends State
  }
  class Parser(input: Input, reporter: Reporter, settings: Settings) {
    private val text = input.text
    private def newPos(start: Int, end: Int): Position = {
      Position.Range(input, start, end)
    }
    private def newText(start: Int, end: Int): Text = {
      val adaptedEnd = math.max(start, end)
      val part = Text(text.substring(start, adaptedEnd))
      part.pos = newPos(start, adaptedEnd)
      part
    }
    private def newCodeFence(
        state: State.CodeFence,
        backtickStart: Int,
        backtickEnd: Int
    ): CodeFence = {
      // tag is characters preceding the code fence in this line
      val tag = newText(state.start, state.start + state.indent)
      val open = newText(state.start, state.start + state.indent + state.backticks.length())
        .dropLinePrefix(state.indent)
      val info = newText(open.pos.end, open.pos.end + state.info.length())
      val adaptedBacktickStart = math.max(0, backtickStart - 1)
      val body = newText(info.pos.end, adaptedBacktickStart).dropLinePrefix(state.indent)
      val close = newText(adaptedBacktickStart, backtickEnd).dropLinePrefix(state.indent)
      val part = CodeFence(open, info, body, close, tag)
      part.pos = newPos(state.start, backtickEnd)
      part
    }
    def acceptParts(): List[MarkdownPart] = {
      var state: State = State.Text
      val parts = mutable.ListBuffer.empty[MarkdownPart]
      var curr = 0
      text.linesWithSeparators.foreach { line =>
        val end = curr + line.length()
        state match {
          case State.Text =>
            val start = line.indexOf("```")
            if (start == 0 || (start > 0 && settings.allowCodeFenceIndented)) {
              val fence = line.substring(start)
              val backticks = fence.takeWhile(_ == '`')
              val info = fence.substring(backticks.length())
              state = State.CodeFence(curr, backticks, info, indent = start)
            } else {
              // TODO Consider putting conditional inside the block
              parts ++= parseLineWithInlineCode(line)
            }
          case s: State.CodeFence =>
            val start = line.indexOf(s.backticks)
            if (
              start == s.indent &&
              line.forall(ch => ch == '`' || ch.isWhitespace)
            ) {
              parts += newCodeFence(s, curr, end)
              state = State.Text
            }
        }
        curr += line.length()
      }
      state match {
        case s: State.CodeFence =>
          parts += newCodeFence(s, text.length(), text.length())
        case _ =>
      }
      parts.toList
    }
  }
  def parse(
      input: Input,
      file: InputFile,
      reporter: Reporter,
      settings: Settings
  ): MarkdownFile = {
    val parser = new Parser(input, reporter, settings)
    val parts = parser.acceptParts()
    MarkdownFile(input, file, parts)
  }

  def parseLineWithInlineCode(line: String): List[MarkdownPart] = {
    if (!line.contains("`")) {
      List(Text(line))
    } else {
      /** TODO
       *    - How should we handle:
       *      - Multiple ticks in a row
       *        - Especially when at the beginning of the line, as other tests already focus on that case.
       *        - Unbalanced tick marks
       *     -
       */
      val prefix = if (line.startsWith("`")) " " else ""
      val suffix = if (line.endsWith("`")) " " else ""
      val tickSections = (prefix + line + suffix).split("`")//.filterNot(s => s.isBlank)
      if (line.contains("Inline")) {
        tickSections.foreach(section => println("Section: " + section))
      }
      if (tickSections.nonEmpty && tickSections.size % 2 == 0)
        throw new RuntimeException("TODO How to handle Unbalanced ticks!")

      tickSections.toList.zipWithIndex.map { case (piece, index) =>
        // TODO This might be dangerous. If the paragraph starts with "scala mdoc", outside of ticks, this
        // could go haywire
        if (index % 2 != 0) {
          if (piece.startsWith("scala mdoc")) {
            val wordsInMdocPiece = piece.split("\\s+")
            val (info, body) = wordsInMdocPiece.splitAt(2)
            InlineMdoc(Text(info.mkString(" ")), Text(body.mkString(" ")))
          } else {
            Text(s"`$piece`") // TODO Any cleaner way of avoiding re-adding backticks here?

          }
        }
        else
          Text(s"$piece") // TODO Any cleaner way of avoiding re-adding backticks here?
      }
    }
  }

}

sealed abstract class MarkdownPart {
  import MarkdownFile.syntax._

  var pos: Position = Position.None
  final def renderToString(out: StringBuilder): Unit =
    this match {
      case Text(value) =>
        out.append(value)
      case fence: CodeFence =>
        val indentation = if (fence.hasBlankTag) fence.tag.value else " " * fence.indent
        fence.newPart match {
          case Some(newPart) =>
            out.appendLinesPrefixed(indentation, newPart)
          case None =>
            fence.tag.renderToString(out)
            fence.openBackticks.renderToString(out)
            fence.newInfo match {
              case None =>
                fence.info.renderToString(out)
              case Some(newInfo) =>
                out.append(newInfo)
                if (!newInfo.endsWith("\n")) {
                  out.append("\n")
                }
            }
            fence.newBody match {
              case None =>
                out.appendLinesPrefixed(indentation, fence.body.value)
              case Some(newBody) =>
                out.appendLinesPrefixed(indentation, newBody)
            }
            out.appendLinesPrefixed(indentation, fence.closeBackticks.value)
        }
      case inlineMdoc: InlineMdoc =>
        out.append("`")
        inlineMdoc.body.renderToString(out)
        out.append("`")
//        out.append(inlineMdoc.body)
    }
}
final case class Text(value: String) extends MarkdownPart {
  import MarkdownFile.syntax._

  def dropLinePrefix(indent: Int): Text = {
    if (indent > 0) {
      val updatedValue = value.linesWithSeparators.map { line =>
        if (!line.isNL && line.length >= indent) line.substring(indent) else line
      }.mkString
      val updatedText = Text(updatedValue)
      updatedText.pos = this.pos
      updatedText
    } else this
  }
}
final case class CodeFence(
    openBackticks: Text,
    info: Text,
    body: Text,
    closeBackticks: Text,
    tag: Text = Text("")
) extends MarkdownPart {
  var newPart = Option.empty[String]
  var newInfo = Option.empty[String]
  var newBody = Option.empty[String]
  def indent: Int = tag.value.length
  def hasBlankTag: Boolean = tag.value.forall(_.isWhitespace)
}

// TODO Info/modifiers
final case class InlineCode(body: Text) extends MarkdownPart
final case class InlineMdoc(info: Text, body: Text) extends MarkdownPart {
  val closeTick = "`"
  // TODO See which vars are necessary
  // Since we're not messing with output, I think these can actually go away
  var newPart = Option.empty[String]
  var newInfo = Option.empty[String]
  var newBody = Option.empty[String]
}
