package vork.website

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta.inputs.Input
import scala.meta.io.RelativePath
import vork.Reporter
import vork.StringModifier
import vork.internal.cli.Context
import vork.internal.io.ConsoleReporter
import vork.internal.markdown.Markdown
import vork.internal.markdown.MarkdownLinks
import vork.internal.markdown.MarkdownLinter

class VorkStringModifier(context: Context) extends StringModifier {
  private val myStdout = new ByteArrayOutputStream()
  private val myReporter = new ConsoleReporter(new PrintStream(myStdout))
  private val markdownSettings = Markdown.vorkSettings(context.copy(reporter = myReporter))
  override val name: String = "vork"
  override def process(info: String, code: Input, reporter: Reporter): String = {
    myStdout.reset()
    myReporter.reset()
    val markdown = Markdown.toMarkdown(code, markdownSettings, myReporter, context.settings)
    val links = MarkdownLinks.fromMarkdown(RelativePath("readme.md"), code)
    MarkdownLinter.lint(List(links), myReporter)
    val stdout = fansi.Str(myStdout.toString()).plainText
    if (myReporter.hasErrors || myReporter.hasWarnings) {
      if (info != "crash") {
        sys.error(stdout)
      }
      s"""
Before:
````
${code.text}
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
${code.text}
````
After:
````
${markdown.trim}
````
"""
    }
  }
}
