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
      variables: Map[String, String],
      reporter: Reporter,
      settings: Settings
  ): Input.VirtualFile = {
    val text = Variable.replaceAllIn(
      input.text,
      { m =>
        input.chars.lift(m.start - 1) match {
          case Some('@') =>
            Position.Range(input, m.start + 1, m.end).text
          case _ =>
            val key = m.group(1)
            variables.get(key) match {
              case Some(value) => value
              case None =>
                val pos = Position.Range(input, m.start, m.end)
                reporter.error(pos, s"key not found: $key")
                pos.text
            }
        }
      }
    )
    Input.VirtualFile(input.toFilename(settings), text)
  }
}
