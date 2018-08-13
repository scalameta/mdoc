package vork.utils

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import vork.Args
import vork.InputFile

object IO {

  def foreachFile(args: Args)(fn: InputFile => Unit): Unit = {
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

  def cleanTarget(options: Args): Unit = {
    if (!options.cleanTarget || !Files.exists(options.out.toNIO)) return
    Files.walkFileTree(options.out.toNIO, deleteVisitor)
  }
}
