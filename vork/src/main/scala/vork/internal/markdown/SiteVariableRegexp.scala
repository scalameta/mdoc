package vork.internal.markdown

import scala.meta.inputs.Input
import scala.meta.inputs.Position
import vork.Reporter
import vork.internal.pos.PositionSyntax._

object SiteVariableRegexp {
  private val Variable = """@(\w+)@""".r
  private val defaultReplacements = Map("" -> "@")
  def replaceVariables(
      input: Input,
      variables: Map[String, String],
      reporter: Reporter
  ): Input.VirtualFile = {
    val map = variables ++ defaultReplacements
    val text = Variable.replaceAllIn(
      input.text, { m =>
        val key = m.group(1)
        map.get(key) match {
          case Some(value) => value
          case None =>
            input.chars.lift(m.start - 1) match {
              case Some('@') =>
                Position.Range(input, m.start + 1, m.end).text
              case _ =>
                val pos = Position.Range(input, m.start, m.end)
                reporter.error(pos, s"key not found: $key")
                pos.text
            }
        }
      }
    )
    Input.VirtualFile(input.filename, text)
  }
}
