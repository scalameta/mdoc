package fox

import io.circe._, syntax._
import fox.Markdown.Site

object Search {
  case class Index(docs: Seq[Section])
  object Index {
    implicit val encoder: ObjectEncoder[Index] =
      Encoder.forProduct1("docs")(_.docs)
  }

  case class Section(location: String, title: String, text: String)
  object Section {
    implicit val encoder: ObjectEncoder[Section] =
      Encoder.forProduct3("location", "text", "title")(
        page => (page.location, page.text, page.title)
      )
  }

  // TODO(olafur) replace this with proper JSON library, it's totally
  // not worth it rolling this by hand.
  def index(options: Options, site: Site): String = {
    val sections = for {
      doc <- site.docs
      header <- doc.headers
    } yield
      Section(
        location = options.href(doc) + header.target,
        title = header.title,
        text = header.text
      )
    Index(sections).asJson.noSpaces
  }

}
