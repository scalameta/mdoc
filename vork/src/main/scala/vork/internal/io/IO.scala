package vork.internal.io

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.meta.io.AbsolutePath
import vork.internal.cli.InputFile
import vork.internal.cli.Settings

object IO {

  def foreachFile(args: Settings)(fn: InputFile => Unit): Unit = {
    implicit val cwd = args.cwd
    val root = args.in.toNIO
    val visitor = new SimpleFileVisitor[Path] {
      override def visitFile(
          file: Path,
          attrs: BasicFileAttributes
      ): FileVisitResult = {
        args.toInputFile(AbsolutePath(file)) match {
          case Some(inputFile) =>
            fn(inputFile)
          case None =>
            () // excluded
        }
        FileVisitResult.CONTINUE
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

  def cleanTarget(options: Settings): Unit = {
    if (!options.cleanTarget || !Files.exists(options.out.toNIO)) return
    Files.walkFileTree(options.out.toNIO, deleteVisitor)
  }
}
