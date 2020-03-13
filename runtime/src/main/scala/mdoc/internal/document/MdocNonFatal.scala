package mdoc.internal.document

import scala.util.control.NonFatal

object MdocNonFatal {
  // NOTE(olafur): Treat all exceptions as non-fatal when evaluating user-code
  // to avoid issues like https://github.com/scalameta/metals/issues/1456
  def unapply(e: Throwable): Option[Throwable] = Some(e)
}
