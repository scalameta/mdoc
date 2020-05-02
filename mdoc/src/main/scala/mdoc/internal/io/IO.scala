package mdoc.internal.io

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings

object IO {

  def foreachOutput(settings: Settings)(fn: (AbsolutePath, RelativePath) => Unit): Unit = {
    settings.out.foreach { root =>
      Files.walkFileTree(
        root.toNIO,
        new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            val path = AbsolutePath(file)
            val relpath = path.toRelative(root)
            if (!settings.isExplicitlyExcluded(relpath)) {
              fn(path, relpath)
            }
            FileVisitResult.CONTINUE
          }
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            val relpath = AbsolutePath(dir).toRelative(root)
            if (settings.isExplicitlyExcluded(relpath)) FileVisitResult.SKIP_SUBTREE
            else FileVisitResult.CONTINUE
          }
        }
      )
    }
  }
  def inputFiles(settings: Settings): List[InputFile] = {
    val buf = List.newBuilder[InputFile]
    foreachInputFile(settings)(buf += _)
    buf.result()
  }

  def foreachInputFile(settings: Settings)(fn: InputFile => Unit): Unit = {
    implicit val cwd = settings.cwd
    def handle(file: AbsolutePath): Unit =
      settings.toInputFile(file).foreach(fn)
    settings.in.foreach { root =>
      val visitor = new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          handle(AbsolutePath(file))
          FileVisitResult.CONTINUE
        }

        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          val relpath = RelativePath(root.toNIO.relativize(dir))
          if (settings.isExplicitlyExcluded(relpath)) FileVisitResult.SKIP_SUBTREE
          else FileVisitResult.CONTINUE
        }
      }
      if (root.isFile) {
        handle(root)
      } else if (root.isDirectory) {
        Files.walkFileTree(root.toNIO, visitor)
      }
    }
  }

  val deleteVisitor: SimpleFileVisitor[Path] = new SimpleFileVisitor[Path] {
    override def visitFile(
        file: Path,
        attrs: BasicFileAttributes
    ): FileVisitResult = {
      Files.delete(file)
      FileVisitResult.CONTINUE
    }
    override def postVisitDirectory(
        dir: Path,
        exc: IOException
    ): FileVisitResult = {
      Files.delete(dir)
      FileVisitResult.CONTINUE
    }
  }

  def cleanTarget(dir: AbsolutePath): Unit = {
    Files.walkFileTree(dir.toNIO, deleteVisitor)
  }
}
