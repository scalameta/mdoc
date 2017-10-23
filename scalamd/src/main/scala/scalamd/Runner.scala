package scalamd

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.GenSeq
import scala.collection.parallel.mutable.ParArray
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.NodeVisitor
import com.vladsch.flexmark.ast.VisitHandler
import com.vladsch.flexmark.ast.Visitor
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.util.NodeTracker
import Markdown._
import com.vladsch.flexmark.util.options.MutableDataSet

class Runner(
    options: Options,
    mdSettings: MutableDataSet,
    logger: Logger
) {
  private val parser = Parser.builder(mdSettings).build
  private val renderer = HtmlRenderer.builder(mdSettings).build
  def collectInputPaths: GenSeq[Path] = {
    val paths = List.newBuilder[Path]
    Files.walkFileTree(
      options.inPath,
      new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (Files.isRegularFile(file)
            && file.getFileName.toString.endsWith(".md")) {
            paths += options.inPath.relativize(file)
          }
          FileVisitResult.CONTINUE
        }
      }
    )
    paths.result()
  }


  def handlePath(path: Path): Unit = {
    val source = options.resolveIn(path)
    val compiled = TutCompiler.compile(source, options)
    val md = parser.parse(compiled)
    val headers = collect[Heading, Header](md) { case h: Heading => Header(h) }
    val html = renderer.render(md)
    val target = options.resolveOut(path)
    Files.createDirectories(target.getParent)
    Files.write(target, html.getBytes(options.charset))
    logger.info(
      s"Compiled ${options.pretty(source)} => ${options.pretty(target)}"
    )
  }

  class FileError(path: Path, cause: Throwable)
      extends Exception(path.toString) {
    override def getStackTrace: Array[StackTraceElement] = Array.empty
    override def getCause: Throwable = cause
  }

  def run(): Unit = {
    val paths = collectInputPaths
    if (paths.isEmpty) {
      logger.error(s"${options.inPath} contains no .md files!")
    } else {
      paths.foreach { path =>
        try {
          handlePath(path)
        } catch {
          case NonFatal(e) =>
            new FileError(path, e).printStackTrace()
        }
      }
    }
  }
}
