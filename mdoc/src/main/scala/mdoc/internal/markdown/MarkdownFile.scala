package mdoc.internal.markdown

import scala.meta.inputs.Position
import scala.meta.inputs.Input
import mdoc.Reporter
import scala.collection.mutable
import scala.meta.io.RelativePath
import mdoc.internal.cli.InputFile

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
  sealed abstract class State
  object State {
    case class CodeFence(start: Int, backticks: String, info: String, indent: Int) extends State
    case object Text extends State
  }
  class Parser(input: Input, reporter: Reporter) {
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
            if (start > -1) {
              val outerPart = line.substring(0, start)
              val fence = line.substring(start)
              val backticks = fence.takeWhile(_ == '`')
              val info = fence.substring(backticks.length())
              state = State.CodeFence(curr, backticks, info, indent = outerPart.length)
            } else {
              parts += newText(curr, end)
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
  def parse(input: Input, file: InputFile, reporter: Reporter): MarkdownFile = {
    val parser = new Parser(input, reporter)
    val parts = parser.acceptParts()
    MarkdownFile(input, file, parts)
  }
}

sealed abstract class MarkdownPart {
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
    }
}
final case class Text(value: String) extends MarkdownPart {
  def dropLinePrefix(indent: Int): Text = {
    if (indent > 0) {
      val updatedValue = value.linesWithSeparators.zipWithIndex.map { case (line, i) =>
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
  def hasBlankTag: Boolean = tag.value.isBlank
}
