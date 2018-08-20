package vork.internal.io

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import java.io.InputStream
import java.nio.file.Files
import java.util.Scanner
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import scala.meta.io.AbsolutePath
import scala.collection.JavaConverters._

final class VorkFileListener(
    executor: ExecutorService,
    in: InputStream,
    runAction: DirectoryChangeEvent => Unit
) extends DirectoryChangeListener {
  private var myIsWatching: Boolean = true
  private var watcher: DirectoryWatcher = _
  private def blockUntilEnterKey(): Unit = {
    try {
      new Scanner(in).nextLine()
    } catch {
      case _: NoSuchElementException =>
    }
  }
  def watchUntilInterrupted(): Unit = {
    watcher.watchAsync(executor)
    blockUntilEnterKey()
    executor.shutdown()
    myIsWatching = false
    watcher.close()
  }
  override def isWatching: Boolean = myIsWatching
  override def onEvent(event: DirectoryChangeEvent): Unit = {
    val targetFile = event.path()
    if (Files.isRegularFile(targetFile)) {
      event.eventType() match {
        case EventType.CREATE => runAction(event)
        case EventType.MODIFY => runAction(event)
        case EventType.OVERFLOW => runAction(event)
        case EventType.DELETE => () // We don't do anything when a file is deleted
      }
    }
  }
}

object VorkFileListener {
  def create(dir: AbsolutePath, executor: ExecutorService, in: InputStream)(
      runAction: DirectoryChangeEvent => Unit
  ): VorkFileListener = {
    val listener = new VorkFileListener(executor, in, runAction)
    val watcher = DirectoryWatcher.create(List(dir.toNIO).asJava, listener, true)
    listener.watcher = watcher
    listener
  }

}
