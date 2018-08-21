package mdoc.website

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta.inputs.Input
import scala.meta.io.RelativePath
import mdoc.Reporter
import mdoc.StringModifier
import mdoc.internal.cli.Context
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Markdown
import mdoc.internal.markdown.MarkdownLinks
import mdoc.internal.markdown.MarkdownLinter
import mdoc.internal.pos.PositionSyntax._

class MdocModifier(context: Context) extends StringModifier {
  private val myStdout = new ByteArrayOutputStream()
  private val myReporter = new ConsoleReporter(new PrintStream(myStdout))
  private val markdownSettings = Markdown.mdocSettings(context.copy(reporter = myReporter))
  override val name: String = "mdoc"
  override def process(info: String, code: Input, reporter: Reporter): String = {
    myStdout.reset()
    myReporter.reset()
    val cleanInput = Input.VirtualFile(code.filename, code.text)
    val markdown = Markdown.toMarkdown(cleanInput, markdownSettings, myReporter, context.settings)
    val links = MarkdownLinks.fromMarkdown(RelativePath("readme.md"), cleanInput)
    MarkdownLinter.lint(List(links), myReporter)
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
