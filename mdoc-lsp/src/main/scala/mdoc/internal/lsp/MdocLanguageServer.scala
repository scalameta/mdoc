package mdoc.internal.lsp

import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import MdocEnrichments._
import com.sun.xml.internal.ws.util.CompletedFuture
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.options.MutableDataSet
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import mdoc.internal.cli.Context
import mdoc.internal.cli.Settings
import mdoc.internal.livereload.SimpleHtml
import mdoc.internal.livereload.TableOfContents
import mdoc.internal.livereload.UndertowLiveReload
import mdoc.internal.markdown.Markdown
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import scala.concurrent.ExecutionContext
import scala.meta.io.AbsolutePath

class MdocLanguageServer(
    ec: ExecutionContext
) {
  val sh = Executors.newSingleThreadScheduledExecutor()
  var workspace: AbsolutePath = _
  var client: MdocLanguageClient = _
  var context: Context = _
  var reporter: DiagnosticsReporter = _
  var settings: Settings = _
  var livereload: UndertowLiveReload = _
  val currentPreview = new AtomicReference[String](
    """
      |<h2>Loading...</h2>
    """.stripMargin
  )
  val currentToc = new AtomicReference[TableOfContents](TableOfContents("", "", 0, None))
  val tmp = AbsolutePath(Files.createTempDirectory("mdoc"))
  tmp.toFile.deleteOnExit()

  def connect(client: MdocLanguageClient): Unit = {
    this.client = client
  }

  @JsonRequest("initialize")
  def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {
    CompletableFuture.completedFuture {
      val capabilities = new ServerCapabilities
      workspace = params.getRootUri.toAbsolutePath
      settings = Settings
        .default(workspace)
        .copy(
          in = workspace,
          out = tmp
        )
      reporter = new DiagnosticsReporter(client)
      context = settings.validate(reporter).get
      livereload = UndertowLiveReload(
        workspace.toNIO,
        reporter = reporter,
        lastPreview = () => currentPreview.get()
      )
      livereload.start()
      capabilities.setTextDocumentSync(TextDocumentSyncKind.Full)
      new InitializeResult(capabilities)
    }
  }
  @JsonNotification("$setTraceNotification")
  def setTraceNotification(): Unit = ()
  @JsonNotification("initialized")
  def initialized(params: InitializedParams): Unit = ()
  @JsonRequest("shutdown")
  def shutdown(): CompletableFuture[Unit] = {
    CompletableFutures.computeAsync { _ =>
      sh.shutdown()
      livereload.stop()
    }
  }
  @JsonNotification("exit")
  def exit(): Unit = {
    System.exit(0)
  }
  @JsonNotification("textDocument/didOpen")
  def didOpen(params: DidOpenTextDocumentParams): Unit = ()
  @JsonNotification("textDocument/didChange")
  def didChange(params: DidChangeTextDocumentParams): Unit = ()

  def previewMarkdown(uri: String): Unit = {
    val input = uri.toInput
    scribe.info(s"Compiling ${input.path}")
    context.reporter.reset()
    client.status(MdocStatusParams(s"Compiling...", show = true))
    val abspath = uri.toAbsolutePath
    val relpath = abspath.toRelative(workspace)
    val markdown = Markdown.mdocSettings(context)
    markdown.set(Markdown.RelativePathKey, Some(relpath))
    markdown.set(Markdown.InputKey, Some(input))
    val document = Markdown.toDocument(input, markdown, reporter, settings)
    val renderer = HtmlRenderer.builder(markdown).build()
    val body = renderer.render(document)
    val toc = TableOfContents(document)
    val filename = abspath.toNIO.getFileName.toString
    val html = PreviewHtml.wrapHtmlBody(body, toc, filename, livereload.url)
    currentPreview.set(html)
    currentToc.set(toc)
    reporter.publishDiagnostics(abspath)
    if (!reporter.hasErrors) {
      client.preview(html)
    }
    client.status(MdocStatusParams("", hide = true))
  }

  @JsonRequest("mdoc/index")
  def index(uri: String): CompletableFuture[String] = {
    sh.schedule(new Runnable {
      def run(): Unit = preview(uri)
    }, 100, TimeUnit.MILLISECONDS)
    val html = PreviewHtml.wrapHtmlBody(
      currentPreview.get(),
      currentToc.get(),
      "mdoc preview",
      livereload.url
    )
    CompletableFuture.completedFuture(html)
  }
  @JsonNotification("mdoc/preview")
  def preview(uri: String): Unit = {
    if (uri.endsWith(".md")) {
      previewMarkdown(uri)
    }
  }

  @JsonNotification("textDocument/didSave")
  def didSave(params: DidSaveTextDocumentParams): Unit = {
    preview(params.getTextDocument.getUri)
  }
  @JsonNotification("textDocument/didClose")
  def didClose(params: DidCloseTextDocumentParams): Unit = ()
  @JsonNotification("workspace/didChangeConfiguration")
  def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = {
    // TODO(olafur): Handle notification changes.
  }
}
