package mdoc.internal.document

import scala.util.control.NonFatal

object MdocNonFatal {
  def unapply(e: Throwable): Option[Throwable] = e match {
    case NonFatal(_) => Some(e)
    // This exception happens when a val of an object fails to initialize,
    // which can happen for any val in an mdoc code fence.
    case _: ExceptionInInitializerError => Some(e)
    case _ => None
  }
}
