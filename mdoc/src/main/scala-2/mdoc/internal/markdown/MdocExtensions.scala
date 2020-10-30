package mdoc.internal.markdown

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.ins.InsExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.SimTocExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.util.misc.Extension
import mdoc.internal.cli.Context

object MdocExtensions {

  /** Defines mdoc's markdown extensions.
    *
    *  Inspired by CommonMark formatter options so that pandoc understands it.
    *  https://github.com/vsch/flexmark-java/blob/master/flexmark-java-samples/src/com/vladsch/flexmark/samples/FormatConverterCommonMark.java#L27-L37
    *
    * @return A sequence of extensions to be applied to Flexmark's options.
    */
  def mdoc(context: Context): List[Extension] = {
    plain
  }

  def plain: List[Extension] = {
    List(
      YamlFrontMatterExtension.create(),
      DefinitionExtension.create(),
      AutolinkExtension.create(),
      TablesExtension.create(),
      EmojiExtension.create(),
      StrikethroughExtension.create(),
      TocExtension.create(),
      InsExtension.create(),
      SimTocExtension.create(),
      WikiLinkExtension.create()
    )
  }

}
