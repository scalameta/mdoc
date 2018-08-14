package vork.internal.markdown

import scala.meta.inputs.Input
import scala.meta.inputs.Position
import vork.Reporter

object SiteVariableRegexp {
  private val Variable = """@(\w+)@""".r
  private val defaultReplacements = Map("" -> "@")
  def replaceVariables(
      input: Input.VirtualFile,
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
            val pos = Position.Range(input, m.start, m.end)
            reporter.error(pos, s"key not found: $key")
            pos.text
        }
      }
    )
    Input.VirtualFile(input.path, text)
  }
}
