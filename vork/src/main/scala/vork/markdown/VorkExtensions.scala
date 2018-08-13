package vork.markdown

import vork.Options
import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.{
  StrikethroughExtension,
  StrikethroughSubscriptExtension
}
import com.vladsch.flexmark.ext.ins.InsExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.{SimTocExtension, TocExtension}
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import vork.Context

object VorkExtensions {

  /**
    * Defines vork's markdown extensions.
    *
    *  Inspired by CommonMark formatter options so that pandoc understands it.
    *  https://github.com/vsch/flexmark-java/blob/master/flexmark-java-samples/src/com/vladsch/flexmark/samples/FormatConverterCommonMark.java#L27-L37
    *
    * @return A sequence of extensions to be applied to Flexmark's options.
    */
  def default(context: Context): java.lang.Iterable[Extension] = {
    import scala.collection.JavaConverters._
    List(
      DefinitionExtension.create(),
      AutolinkExtension.create(),
      VorkParserExtension.create(context),
      VorkFormatterExtension.create(context.options),
      TablesExtension.create(),
      EmojiExtension.create(),
      FootnoteExtension.create(),
      StrikethroughExtension.create(),
      TocExtension.create(),
      InsExtension.create(),
      SimTocExtension.create(),
      WikiLinkExtension.create(),
    ).asJava
  }

}
