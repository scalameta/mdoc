package vork.internal.io

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import vork.internal.cli.InputFile
import vork.internal.cli.Settings

object IO {

  def foreachFile(settings: Settings)(fn: InputFile => Unit): Unit = {
    implicit val cwd = settings.cwd
    val root = settings.in.toNIO
    val visitor = new SimpleFileVisitor[Path] {
      override def visitFile(
          file: Path,
          attrs: BasicFileAttributes
      ): FileVisitResult = {
        settings.toInputFile(AbsolutePath(file)) match {
          case Some(inputFile) =>
            fn(inputFile)
          case None =>
            () // excluded
        }
        FileVisitResult.CONTINUE
      }

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val relpath = RelativePath(root.relativize(dir))
        if (dir == root || settings.matches(relpath)) FileVisitResult.CONTINUE
        else FileVisitResult.SKIP_SUBTREE // excluded
      }
    }
    Files.walkFileTree(root, visitor)
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
