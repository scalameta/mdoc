package vork.utils

import java.nio.file.Path
import scala.meta.io.AbsolutePath
import scala.util.matching.Regex
import metaconfig._

case class FilterMatcher(
    includeFilters: Regex,
    excludeFilters: Regex
) {
  def matches(file: AbsolutePath): Boolean = matches(file.toString())
  def matches(path: Path): Boolean = matches(path.toString)
  def matches(input: String): Boolean =
    includeFilters.findFirstIn(input).isDefined &&
      excludeFilters.findFirstIn(input).isEmpty
  def unapply(arg: String): Boolean =
    matches(arg)
}

object FilterMatcher {
  lazy val matchEverythingRegex: Regex = ".*".r
  lazy val matchNothingRegex: Regex = mkRegexp(Nil)
  lazy val matchEverything: FilterMatcher =
    new FilterMatcher(includeFilters = matchEverythingRegex, excludeFilters = matchNothingRegex)
  lazy val matchNothing: FilterMatcher =
    new FilterMatcher(includeFilters = matchNothingRegex, excludeFilters = matchEverythingRegex)

  def mkRegexp(filters: Seq[String]): Regex =
    filters match {
      case Nil => "$a".r // will never match anything
      case head :: Nil => head.r
      case _ => filters.mkString("(", "|", ")").r
    }

  def apply(includes: Seq[String], excludes: Seq[String]): FilterMatcher =
    new FilterMatcher(mkRegexp(includes), mkRegexp(excludes))
  def apply(include: String): FilterMatcher =
    new FilterMatcher(mkRegexp(Seq(include)), mkRegexp(Nil))
  def apply(include: Option[Regex], exclude: Option[Regex]): FilterMatcher =
    new FilterMatcher(include.getOrElse(matchEverythingRegex), exclude.getOrElse(matchNothingRegex))
}
