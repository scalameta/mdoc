package mdoc

import scala.meta.inputs.Position

trait Reporter {
  def error(throwable: Throwable): Unit
  def error(pos: Position, throwable: Throwable): Unit
  def error(pos: Position, msg: String): Unit
  def error(msg: String): Unit
  def warning(pos: Position, msg: String): Unit
  def warning(msg: String): Unit
  def info(pos: Position, msg: String): Unit
  def info(msg: String): Unit
  def print(msg: String): Unit
  def println(msg: String): Unit

  private[mdoc] def hasWarnings: Boolean
  private[mdoc] def hasErrors: Boolean
  private[mdoc] def warningCount: Int
  private[mdoc] def errorCount: Int
  private[mdoc] def reset(): Unit
}
