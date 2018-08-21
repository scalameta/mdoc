package mdoc.internal.markdown

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.util.options.MutableDataHolder
import mdoc.internal.cli.Settings

// To be used later, it adds extensions to the formatter
class MdocFormatterExtension(options: Settings) extends Formatter.FormatterExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = ()
  override def extend(builder: Formatter.Builder): Unit = ()
}

object MdocFormatterExtension {
  def create(options: Settings): Extension = new MdocFormatterExtension(options)
}
