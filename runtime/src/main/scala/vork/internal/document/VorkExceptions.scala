package mdoc.internal.document

object MdocExceptions {
  def trimStacktrace(e: Throwable): Unit = {
    val stacktrace = e.getStackTrace.takeWhile(!_.getClassName.startsWith("mdoc"))
    e.setStackTrace(stacktrace)
  }
}
