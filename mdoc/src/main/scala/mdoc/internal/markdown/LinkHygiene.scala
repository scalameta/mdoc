package mdoc.internal.markdown

import java.net.URI
import mdoc.Reporter

object LinkHygiene {
  def lint(docs: List[DocumentLinks], reporter: Reporter): Unit = {
    val isValidHeading = docs.iterator.flatMap(_.absoluteDefinitions).toSet
    for {
      doc <- docs
      enclosingDocument = doc.relpath.toURI(false)
      reference <- doc.references
      uri <- resolve(enclosingDocument, reference.url)
      if uri.getScheme == null && uri.getHost == null
      if !isValidHeading(uri)
    } {
      val isAbsolutePath = uri.getPath.startsWith("/")
      val hint =
        if (isAbsolutePath) ". To fix this problem, make the link relative."
        else ""
      reporter.warning(reference.pos, s"Reference '$uri' does not exist$hint")
    }
  }

  private def resolve(baseUri: URI, reference: String): Option[URI] = {
    try {
      Some(baseUri.resolve(reference).normalize())
    } catch {
      case _: IllegalArgumentException =>
        // Il
        None
    }
  }

}
