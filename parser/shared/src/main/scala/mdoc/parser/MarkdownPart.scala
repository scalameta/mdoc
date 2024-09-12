package mdoc.parser

import scala.collection.mutable

object MarkdownPart {
  object syntax {
    private[mdoc] implicit class StringOps(private val x: String) extends AnyVal {
      def isNL: Boolean = x.forall { c => c == '\n' || c == '\r' }
    }

    private[mdoc] implicit class StringBuilderOps(private val x: StringBuilder) extends AnyVal {
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
  class Parser(text: String, settings: ParserSettings) {
    private def newText(start: Int, end: Int): Text = {
      val adaptedEnd = math.max(start, end)
      val part = Text(text.substring(start, adaptedEnd))
      part.posBeg = start
      part.posEnd = adaptedEnd
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
      val info = newText(open.posEnd, open.posEnd + state.info.length())
      val adaptedBacktickStart = math.max(0, backtickStart - 1)
      val body = newText(info.posEnd, adaptedBacktickStart).dropLinePrefix(state.indent)
      val close = newText(adaptedBacktickStart, backtickEnd).dropLinePrefix(state.indent)
      val part = CodeFence(open, info, body, close, tag)
      part.posBeg = state.start
      part.posEnd = backtickEnd
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
  def parse(text: String, settings: ParserSettings): List[MarkdownPart] = {
    new Parser(text, settings).acceptParts()
  }
}

sealed abstract class MarkdownPart {
  import MarkdownPart.syntax._

  var posBeg: Int = 0
  var posEnd: Int = -1

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
  import MarkdownPart.syntax._

  def dropLinePrefix(indent: Int): Text = {
    if (indent > 0) {
      val updatedValue = value.linesWithSeparators.map { line =>
        if (!line.isNL && line.length >= indent) line.substring(indent) else line
      }.mkString
      val updatedText = Text(updatedValue)
      updatedText.posBeg = this.posBeg
      updatedText.posEnd = this.posEnd
      updatedText
    } else this
  }
}

object CodeFence {
  val tag = "scala mdoc"
  val taglen = tag.length
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

  def getMdocMode: Option[String] = {
    val infoStr = info.value
    if (!infoStr.startsWith(CodeFence.tag)) None
    else if (infoStr.length == CodeFence.taglen) Some("")
    else {
      val head = infoStr.charAt(CodeFence.taglen)
      if (head == ':') {
        val idxSpace = infoStr.indexWhere(_.isWhitespace, CodeFence.taglen + 1)
        Some(
          if (idxSpace < 0) infoStr.substring(CodeFence.taglen + 1)
          else infoStr.substring(CodeFence.taglen + 1, idxSpace)
        )
      } else {
        if (head.isWhitespace) Some("") else None
      }
    }
  }
}
