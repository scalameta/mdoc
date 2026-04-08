package mdoc.internal.markdown

import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.Reporter
import mdoc.internal.cli.Settings
import mdoc.internal.pos.PositionSyntax._

object VariableRegex {

  private val Variable = """@(\w+)@""".r

  def replaceVariables(
      input: Input,
      reporter: Reporter,
      settings: Settings
  ): Input.VirtualFile = {
    replaceVariables(input, settings.site, reporter, settings.reportRelativePaths)
  }

  def replaceVariables(
      input: Input,
      variables: Map[String, String],
      reporter: Reporter,
      reportRelativePaths: Boolean
  ): Input.VirtualFile = {
    val text = replaceVariablesInText(input, reporter, variables)
    Input.VirtualFile(input.toFilename(reportRelativePaths), text)
  }

  def replaceVariablesInText(
      input: Input,
      reporter: Reporter,
      variables: Map[String, String]
  ): String = {
    val chars = input.chars
    def text(beg: Int, end: Int): String =
      if (beg >= end) "" else new String(chars, beg, end - beg)
    Variable.replaceAllIn(
      input.text,
      { m =>
        chars.lift(m.start - 1) match {
          case Some('@') => text(m.start + 1, m.end)
          case _ =>
            val key = m.group(1)
            variables.get(key) match {
              case Some(value) => value.replace("$", "\\$")
              case None =>
                val pos = Position.Range(input, m.start, m.end)
                reporter.error(pos, s"key not found: $key")
                text(m.start, m.end)
            }
        }
      }
    )
  }

}
