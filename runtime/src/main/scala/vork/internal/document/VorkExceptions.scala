package vork.internal.document

object VorkExceptions {
  def trimStacktrace(e: Throwable): Unit = {
    val stacktrace = e.getStackTrace.takeWhile(!_.getClassName.startsWith("vork"))
    e.setStackTrace(stacktrace)
  }
}
