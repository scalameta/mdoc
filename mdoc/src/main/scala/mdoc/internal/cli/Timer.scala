package mdoc.internal.cli

import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class Timer() {
  val startNanos: Long = System.nanoTime()
  def elapsedNanos: Long = {
    val now = System.nanoTime()
    now - startNanos
  }
  def elapsedMillis: Long = {
    TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
  }
  def elapsedSeconds: Long = {
    TimeUnit.NANOSECONDS.toSeconds(elapsedNanos)
  }
  override def toString: String = {
    Timer.readableNanos(elapsedNanos)
  }
}

object Timer {
  def readableNanos(nanos: Long): String = {
    val seconds = TimeUnit.NANOSECONDS.toSeconds(nanos)
    if (seconds > 20) readableSeconds(seconds)
    else {
      val ms = TimeUnit.NANOSECONDS.toMillis(nanos).toDouble
      val partialSeconds = ms / 1000
      new DecimalFormat("#.##s").format(partialSeconds)
    }
  }
  def readableSeconds(n: Long): String = {
    val minutes = n / 60
    val seconds = n % 60
    if (minutes > 0) {
      if (seconds == 0) s"${minutes}m"
      else s"${minutes}m${seconds}s"
    } else {
      s"${seconds}s"
    }
  }
}
