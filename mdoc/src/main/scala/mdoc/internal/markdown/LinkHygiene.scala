package mdoc.internal.markdown

import java.net.URI
import mdoc.Reporter

object LinkHygiene {
  def lint(docs: List[DocumentLinks], reporter: Reporter, verbose: Boolean): Unit = {
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
      val debug =
        if (verbose) s". isValidHeading=$isValidHeading"
        else ""
      val hint =
        if (isAbsolutePath)
          s". To fix this problem, either make the link relative or turn it into complete URL such as http://example.com$uri."
        else ""
      reporter.warning(reference.pos, s"Unknown link '$uri'$hint$debug")
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
