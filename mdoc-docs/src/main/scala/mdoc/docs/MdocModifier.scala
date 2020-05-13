package mdoc.docs

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta.inputs.Input
import scala.meta.io.RelativePath
import mdoc.Reporter
import mdoc.StringModifier
import mdoc.internal.cli.Context
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Markdown
import mdoc.internal.markdown.DocumentLinks
import mdoc.internal.markdown.GitHubIdGenerator
import mdoc.internal.markdown.LinkHygiene
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.markdown.MarkdownFile
import mdoc.internal.cli.InputFile

class MdocModifier(context: Context) extends StringModifier {
  private val myStdout = new ByteArrayOutputStream()
  private val myReporter = new ConsoleReporter(new PrintStream(myStdout))
  private val myContext = context.copy(reporter = myReporter)
  override val name: String = "mdoc"
  override def process(info: String, code: Input, reporter: Reporter): String = {
    myStdout.reset()
    myReporter.reset()
    val cleanInput = Input.VirtualFile(code.filename, code.text)
    val file = InputFile.fromRelativeFilename(code.filename, context.settings)
    val markdown = Markdown.toMarkdown(
      cleanInput,
      myContext,
      file,
      myContext.settings.site,
      myReporter,
      myContext.settings
    )
    val links = DocumentLinks.fromMarkdown(GitHubIdGenerator, file.relpath, cleanInput)
    LinkHygiene.lint(List(links), myReporter, verbose = false)
    val stdout = fansi.Str(myStdout.toString()).plainText
    if (myReporter.hasErrors || myReporter.hasWarnings) {
      if (info != "crash") {
        sys.error(stdout)
      }
      s"""
Before:
````
${cleanInput.text}
````
Error:
````
${stdout.trim}
````
"""
    } else {
      s"""
Before:
````
${cleanInput.text}
````
After:
````
${markdown.trim}
````
"""
    }
  }
}
