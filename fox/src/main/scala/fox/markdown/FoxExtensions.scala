package fox.markdown

import fox.Options
import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension

object FoxExtensions {
  def default(options: Options): java.lang.Iterable[Extension] = {
    import scala.collection.JavaConverters._
    List(
      AutolinkExtension.create(),
      FoxParserExtension.create(options),
      FoxAttributeProviderExtension.create(options),
      TablesExtension.create(),
      EmojiExtension.create(),
      FootnoteExtension.create(),
      StrikethroughExtension.create()
    ).asJava
  }
}
