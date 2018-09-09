package mdoc.internal.markdown

import java.net.URI
import mdoc.Reporter
import me.xdrop.fuzzywuzzy.FuzzySearch

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
        if (verbose) {
          val query = uri.toString
          val candidates = isValidHeading
            .map { candidate =>
              val score = FuzzySearch.ratio(candidate.toString, query)
              score -> f"$score%-3s $candidate"
            }
            .toSeq
            .sortBy(-_._1)
            .map(_._2)
            .mkString("\n  ")
          s"\nisValidHeading:\n  $candidates"
        } else ""
      val help = getSimilarHeading(isValidHeading, uri) match {
        case None => "."
        case Some(similar) => s", did you mean '$similar'?"
      }
      val hint =
        if (isAbsolutePath)
          s" To fix this problem, either make the link relative or turn it into complete URL such as http://example.com$uri."
        else ""
      reporter.warning(reference.pos, s"Unknown link '$uri'$help$hint$debug")
    }
  }

  private def getSimilarHeading(candidates: Set[URI], query: URI): Option[URI] = {
    val queryString = query.toString
    val similar = for {
      candidate <- candidates.iterator
      score = FuzzySearch.ratio(queryString, candidate.toString)
      if score > 90 // discard noisy candidates
    } yield score -> candidate
    if (similar.isEmpty) None
    else Some(similar.maxBy(_._1)._2)
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
