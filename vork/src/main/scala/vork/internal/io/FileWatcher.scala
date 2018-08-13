package vork.internal.io

import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.JavaConverters._

object FileWatcher {
  def watch(dirs0: Seq[Path], runAction: DirectoryChangeEvent => Unit): Unit = {
    val dirs = dirs0.distinct
    val dirsAsJava: java.util.List[Path] = dirs.asJava

    // Report on non-existing source directories
    val nonExisting = dirs.filterNot(d => Files.exists(d))
    if (nonExisting.nonEmpty)
      sys.error(s"Expected existing directories ${nonExisting.mkString(", ")}.")

    val watcher = DirectoryWatcher.create(
      dirsAsJava,
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
      }
    )

    try watcher.watch()
    finally watcher.close()
  }
}
