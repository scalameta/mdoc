package vork

import scala.meta.inputs.Position

trait Reporter {
  def hasErrors: Boolean
  def reset(): Unit
  def error(pos: Position, throwable: Throwable): Unit
  def error(pos: Position, msg: String): Unit
  def error(msg: String): Unit
  def info(pos: Position, msg: String): Unit
  def info(msg: String): Unit
  def warning(pos: Position, msg: String): Unit
  def warning(msg: String): Unit
}
