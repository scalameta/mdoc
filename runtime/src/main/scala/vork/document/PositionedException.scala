package vork.document

import scala.util.control.NoStackTrace

final class PositionedException(
    val section: Int,
    val pos: RangePosition,
    val cause: Throwable
) extends Exception(pos.toString, cause)
    with NoStackTrace
