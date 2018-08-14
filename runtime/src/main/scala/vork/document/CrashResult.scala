package vork.document

sealed trait CrashResult
object CrashResult {
  final case class Crashed(e: Throwable, pos: RangePosition) extends CrashResult
  final case class Success(pos: RangePosition) extends CrashResult
}
