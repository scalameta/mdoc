package mdoc.document

import scala.util.control.NoStackTrace

final class DocumentException(
    val sections: List[Section],
    val pos: RangePosition,
    val cause: Throwable
) extends Exception(pos.toString, cause)
    with NoStackTrace
