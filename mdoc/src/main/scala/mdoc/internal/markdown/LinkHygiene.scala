package mdoc.internal.markdown

import java.net.URI
import mdoc.Reporter
import metaconfig.internal.Levenshtein
import scala.meta.inputs.Position

final case class DeadLinkInfo(ref: Position, msg: String)

object LinkHygiene {
  def lint(docs: List[DocumentLinks], verbose: Boolean): List[DeadLinkInfo] = {
    val isValidHeading = docs.iterator.flatMap(_.absoluteDefinitions).toSet
    for {
      doc <- docs
      enclosingDocument = doc.relpath.toURI(false)
      reference <- doc.references
      uri <- resolve(enclosingDocument, reference.url)
      if uri.getScheme == null && uri.getHost == null
      if !isValidHeading(uri)
    } yield {
      val isAbsolutePath = uri.getPath.startsWith("/")
      val debug =
        if (verbose) {
          val headings = isValidHeading.map(_.toString()).toSeq.sorted.mkString("\n  ")
          s"\nisValidHeading:\n  ${headings}"
        } else {
          ""
        }
      val candidates = isValidHeading.map(_.toString()).toSeq
      val help = closestCandidate(uri.toString(), candidates) match {
        case None => "."
        case Some(similar) => s", did you mean '$similar'?"
      }
      val hint =
        if (isAbsolutePath)
          s" To fix this problem, either make the link relative or turn it into complete URL such as http://example.com$uri."
        else ""
      DeadLinkInfo(reference.pos, s"Unknown link '$uri'$help$hint$debug")
    }
  }

  def report(asError: Boolean, deadLinks: List[DeadLinkInfo], reporter: Reporter): Unit = {
    def printDeadLink(print: (Position, String) => Unit)(deadLink: DeadLinkInfo): Unit = {
      val DeadLinkInfo(pos, msg) = deadLink
      print(pos, msg)
    }

    if (asError) {
      deadLinks.foreach { printDeadLink(reporter.error) }
    } else {
      deadLinks.foreach { printDeadLink(reporter.warning) }
    }

  }

  def closestCandidate(
      query: String,
      candidates: Seq[String]
  ): Option[String] = {
    if (candidates.isEmpty) {
      None
    } else {
      val candidate = candidates.sortBy(Levenshtein.distance(query)).head
      val maxLength = query.length() + candidate.length()
      val minDifference = math.abs(query.length() - candidate.length())
      val difference = Levenshtein.distance(candidate)(query).toDouble - minDifference
      val ratio = (maxLength - difference).toDouble / maxLength
      if (ratio > 0.9) Some(candidate)
      else None // Don't return candidate when difference is large.
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
