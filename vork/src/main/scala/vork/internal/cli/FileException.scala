package vork.internal.cli

import scala.meta.io.AbsolutePath
import scala.util.control.NoStackTrace

private class FileException(path: AbsolutePath, cause: Throwable)
    extends Exception(path.toString)
    with NoStackTrace {
  override def getCause: Throwable = cause
}
