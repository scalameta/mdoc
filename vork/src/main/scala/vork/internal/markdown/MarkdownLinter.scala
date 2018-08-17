package vork.internal.markdown

import vork.Reporter
import vork.internal.cli.Settings

object MarkdownLinter {
  def lint(links: List[MarkdownLinks], reporter: Reporter): Unit = {
    val isValidHeading = links.iterator.flatMap(_.absoluteDefinitions).toSet
    for {
      link <- links
      enclosingDocument = link.relpath.toURI(false).toString
      reference <- link.references
      if reference.isInternal
      absurl = reference.toAbsolute(enclosingDocument)
      if !isValidHeading(absurl)
    } {
      reporter.warning(reference.pos, s"Section '${reference.url}' does not exist")
    }
  }

}
