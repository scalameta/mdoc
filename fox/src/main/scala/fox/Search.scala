package fox

import io.circe.Json
import io.circe.JsonObject
import fox.Markdown.Site

object Search {
  def index(options: Options, site: Site): String = {
    val sections = for {
      doc <- site.all
      header <- doc.headers
    } yield {
      val location = options.href(doc, withBase = false) + header.target
      val section: List[(String, Json)] =
        ("location" -> Json.fromString(location)) ::
          ("title" -> Json.fromString(header.title)) ::
          ("text" -> Json.fromString(header.text)) ::
          Nil
      Json.fromFields(section)
    }
    val docs =
      JsonObject.singleton("docs", Json.fromValues(sections.toIterable))
    Json.fromJsonObject(docs).noSpaces
  }
}
