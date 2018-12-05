package mdoc.internal.lsp

import javax.annotation.Nullable
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.services.LanguageClient

trait MdocLanguageClient extends LanguageClient {
  @JsonNotification("mdoc/status")
  def status(params: MdocStatusParams): Unit
  @JsonNotification("mdoc/preview")
  def preview(html: String): Unit
}

case class MdocStatusParams(
    text: String,
    @Nullable show: java.lang.Boolean = null,
    @Nullable hide: java.lang.Boolean = null,
    @Nullable tooltip: String = null,
    @Nullable command: String = null
)
