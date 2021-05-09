package mdoc.internal.io

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import java.io.InputStream
import java.nio.file.Files
import java.util.Scanner
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import scala.meta.io.AbsolutePath
import mdoc.internal.pos.PositionSyntax._

final class MdocFileListener(
    executor: ExecutorService,
    in: InputStream,
    runAction: DirectoryChangeEvent => Unit
) extends DirectoryChangeListener {
  private var myIsWatching: Boolean = true
  private var watcher: DirectoryWatcher = _
  private def blockUntilEnterKey(): Unit = {
    try {
      new Scanner(in).nextLine()
      println("Shutting down...")
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

object MdocFileListener {
  def create(inputs: List[AbsolutePath], executor: ExecutorService, in: InputStream)(
      runAction: DirectoryChangeEvent => Unit
  ): MdocFileListener = {
    val listener = new MdocFileListener(executor, in, runAction)
    val paths = inputs
      .map(_.toNIO)
      .map {
        case file if Files.isRegularFile(file) => file.getParent()
        case path => path
      }
      .distinct
      .asJava
    val watcher = DirectoryWatcher
      .builder()
      .paths(paths)
      .listener(listener)
      // NOTE(olafur): we don't use the built-in file hasher because it's slow
      // on startup for large directories (can take minutes). To prevent
      // duplicate notifications, we implement file hashing only for files the
      // files we're interested in, see MainOps.
      .fileHashing(false)
      .logger(NOPLogger.NOP_LOGGER)
      .build()
    listener.watcher = watcher
    listener
  }

}
