package mdoc.internal.lsp

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
    show: java.lang.Boolean = null,
    hide: java.lang.Boolean = null,
    tooltip: String = null,
    command: String = null
)
