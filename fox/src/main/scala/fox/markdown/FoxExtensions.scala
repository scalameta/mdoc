package fox.markdown

import fox.Options
import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension

object FoxExtensions {
  def default(options: Options): java.lang.Iterable[Extension] = {
    import scala.collection.JavaConverters._
    Iterable(
      AutolinkExtension.create(),
      FoxParserExtension.create(options),
      FoxAttributeProviderExtension.create(options),
      TablesExtension.create(),
      StrikethroughExtension.create()
    ).asJava
  }
}
