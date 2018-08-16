package vork.internal.io

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import java.nio.file.Files
import scala.collection.JavaConverters._
import scala.meta.io.AbsolutePath

object FileWatcher {
  def watch(dir: AbsolutePath, runAction: DirectoryChangeEvent => Unit): Unit = {

    val watcher = DirectoryWatcher.create(
      List(dir.toNIO).asJava,
      new DirectoryChangeListener {
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
      },
      true
    )

    try watcher.watch()
    finally watcher.close()
  }
}
