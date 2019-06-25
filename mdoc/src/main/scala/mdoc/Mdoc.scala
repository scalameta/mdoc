package mdoc

import java.io.File

import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths

import scala.meta.Input
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import mdoc.internal.markdown.Markdown
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import metaconfig.Configured
import mdoc.Reporter

import scala.util.{Try,Success,Failure}


object Mdoc {

  case class MdocContext(fileContents: String, markdown: MutableDataSet, settings: Settings, input: Input, reporter: Reporter, output: ByteArrayOutputStream) {
    def messages = output.toString()
  }

  private def fixEmptyFenceBlocks(md: Iterator[String]): String = md.foldLeft(Vector[String]()) { (priorLines: Vector[String], line: String) =>
    if (!priorLines.isEmpty && priorLines.last.startsWith("```") && line.startsWith("```")) {
      priorLines :+ s"\n$line"
    } else {
      priorLines :+ line
    }
  }.mkString("\n")
  

  private def configure(workingDir: Path, relativePath: String, filename: String, contents: Iterator[String]): MdocContext = {
    val outStream = new ByteArrayOutputStream
    val fixedContents = fixEmptyFenceBlocks(contents)
    val reporter = new ConsoleReporter(new PrintStream(outStream))

    val args = List("--in", ".", "--out", System.getProperty("java.io.tmpdir"))
    val configured = Settings.fromCliArgs(args, Settings.default(AbsolutePath(workingDir)))

    configured.andThen(_.validate(reporter)) match {
      case Configured.NotOk(error) =>
        error.all.foreach(msg => reporter.error(msg))
        throw new IllegalStateException(outStream.toString())
      case Configured.Ok(configured) =>
        val markdown = Markdown.mdocSettings(configured)
        markdown.set(Markdown.RelativePathKey, Some(RelativePath(Paths.get(relativePath))))
        val input = Input.VirtualFile(filename, fixedContents)
        markdown.set(Markdown.InputKey, Some(input))
        MdocContext(fixedContents, markdown, configured.settings, input, reporter, outStream)
    }
  }

  private def mdoc(ctx: MdocContext)(block: => String): String = Try(block) match {
    case Success(output) => output
    case Failure(ex) =>
      val messages = new PrintStream(ctx.output)
      messages.println("""Failure in mdoc processor""")
      ex.printStackTrace(messages)
      messages.flush()
      
      ctx.fileContents
  }

  /** Returns (output, messages) or throws IllegalStateException */
  def markdown(srcDir: Path, relativePath: String, filename: String, contents: Iterator[String]): (String, String) = {
    val ctx = configure(srcDir, relativePath, filename, contents)
    val output = mdoc(ctx) {
      Markdown.toMarkdown(ctx.input, ctx.markdown, ctx.reporter, ctx.settings)
    }
    (output, ctx.messages)
  }

  def markdown(srcDir: Path, relativePath: String, filename: String, contents: String): (String, String) = {
    markdown(srcDir, relativePath, filename, contents.lines)
  }
  def markdown(contents: String): (String, String) = markdown(new File(".").toPath, "", "", contents.lines)

  def markdown(srcDir: Path, relativePath: String, filename: String, contents: Seq[String]): (String, String) = {
    markdown(srcDir, relativePath, filename, contents.iterator)
  }
  def markdown(contents: Seq[String]): (String, String) = markdown(new File(".").toPath, "", "", contents.iterator)


  /** Returns (output, messages) or throws IllegalStateException */
  def html(srcDir: Path, relativePath: String, filename: String, contents: Iterator[String]): (String, String) = {
    val ctx = configure(srcDir, relativePath, filename, contents)
    val output = mdoc(ctx) {
      Markdown.toHtml(ctx.input, ctx.markdown, ctx.reporter, ctx.settings)
    }
    (output, ctx.messages)
  }

  def html(srcDir: Path, relativePath: String, filename: String, contents: String): (String, String) = {
    html(srcDir, relativePath, filename, contents.lines)
  }
  def html(contents: String): (String, String) = html(new File(".").toPath, "", "", contents.lines)

  def html(srcDir: Path, relativePath: String, filename: String, contents: Seq[String]): (String, String) = {
    html(srcDir, relativePath, filename, contents.iterator)
  }
  def html(contents: Seq[String]): (String, String) = html(new File(".").toPath, "", "", contents.iterator)

}
