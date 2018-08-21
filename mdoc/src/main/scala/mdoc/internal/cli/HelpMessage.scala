package mdoc.internal.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.annotation.Hidden
import metaconfig.annotation.Inline
import metaconfig.generic
import metaconfig.internal.Case
import org.typelevel.paiges.Doc
import scala.collection.mutable.ListBuffer

// TODO: upstream to metaconfig
final class HelpMessage[T: generic.Surface: ConfEncoder](
    default: T,
    version: String,
    usage: String,
    description: Doc
) {

  /** Line wrap prose while keeping markdown code fences unchanged. */
  private def markdownish(text: String): Doc = {
    val buf = ListBuffer.empty[String]
    val paragraphs = ListBuffer.empty[Doc]
    var insideCodeFence = false
    def flush(): Unit = {
      if (insideCodeFence) {
        paragraphs += Doc.intercalate(Doc.line, buf.map(Doc.text))
      } else {
        paragraphs += Doc.paragraph(buf.mkString("\n"))
      }
      buf.clear()
    }
    text.lines.foreach { line =>
      if (line.startsWith("```")) {
        flush()
        insideCodeFence = !insideCodeFence
      }
      buf += line
    }
    flush()
    Doc.intercalate(Doc.line, paragraphs)
  }

  def options(width: Int): String = {
    val settings = generic.Settings[T]
    val obj = ConfEncoder[T].writeObj(default)
    val sb = new StringBuilder()
    def printOption(setting: generic.Setting, value: Conf): Unit = {
      if (setting.annotations.exists(_.isInstanceOf[Hidden])) return
      setting.annotations.foreach {
        case section: Section =>
          sb.append("\n")
            .append(section.name)
            .append(":\n")
        case _ =>
      }
      val name = Case.camelToKebab(setting.name)
      sb.append("\n")
        .append("  --")
        .append(name)
      setting.extraNames.foreach { name =>
        if (name.length == 1) {
          sb.append(" | -")
            .append(Case.camelToKebab(name))
        }
      }
      if (!setting.isBoolean) {
        sb.append(" ")
          .append(setting.tpe)
          .append(" (default: ")
          .append(value.toString())
          .append(")")
      }
      sb.append("\n")
      setting.description.foreach { description =>
        sb.append("    ")
          .append(markdownish(description).nested(4).render(width))
          .append('\n')
      }
    }

    settings.settings.zip(obj.values).foreach {
      case (setting, (_, value)) =>
        if (setting.annotations.exists(_.isInstanceOf[Inline])) {
          for {
            underlying <- setting.underlying.toList
            (field, (_, fieldDefault)) <- underlying.settings
              .zip(value.asInstanceOf[Conf.Obj].values)
          } {
            printOption(field, fieldDefault)
          }
        } else {
          printOption(setting, value)
        }
    }
    sb.toString()
  }

  def helpMessage(out: PrintStream, width: Int): Unit = {
    out.println(version)
    out.println(usage)
    out.println(description.render(width))
    out.println(options(width))
  }

  def helpMessage(width: Int): String = {
    val baos = new ByteArrayOutputStream()
    helpMessage(new PrintStream(baos), width)
    baos.toString(StandardCharsets.UTF_8.name())
  }

}
