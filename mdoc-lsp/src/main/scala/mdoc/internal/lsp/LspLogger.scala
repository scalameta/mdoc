package mdoc.internal.lsp

import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import scribe.Level
import scribe.LogRecord
import scribe.Logger
import scribe.format._
import scribe.writer.Writer

/**
  * Scribe logging handler that forwards logging messages to the LSP editor client.
  */
object LspLogger extends Writer {
  def update(client: MdocLanguageClient): Unit = {
    this.languageClient = Some(client)
    Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(
        formatter = defaultFormat,
        minimumLevel = Some(scribe.Level.Info)
      )
      .withHandler(
        writer = LspLogger,
        formatter = defaultFormat,
        minimumLevel = Some(Level.Info)
      )
      .replace()
  }
  def defaultFormat = formatter"$levelPaddedRight $message$newLine"
  var languageClient: Option[MdocLanguageClient] = None
  override def write[M](record: LogRecord[M], output: String): Unit = {
    languageClient.foreach { client =>
      client.logMessage(new MessageParams(MessageType.Log, record.message))
    }
  }
}
