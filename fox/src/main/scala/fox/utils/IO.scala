package fox.utils

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import fox.Options

object IO {

  def absolutize(path: Path, cwd: Path): Path =
    if (path.isAbsolute) path
    else cwd.resolve(path)

  def collectInputPaths(options: Options): List[Path] = {
    val paths = List.newBuilder[Path]
    Files.walkFileTree(
      options.in,
      new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (Files.isRegularFile(file)
            && file.getFileName.toString.endsWith(".md")) {
            paths += options.in.relativize(file)
          }
          FileVisitResult.CONTINUE
        }
      }
    )
    paths.result()
  }

  final val deleteVisitor = new SimpleFileVisitor[Path] {
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

  def cleanTarget(options: Options): Unit = {
    // Clean all this and maybe use better-files as a better replacement?
    if (!options.cleanTarget || !Files.exists(options.out)) return
    Files.walkFileTree(options.out, deleteVisitor)
  }
}
