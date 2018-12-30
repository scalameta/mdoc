package mdoc.internal.lsp

import com.vladsch.flexmark.html.HtmlRenderer
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import mdoc.internal.cli.Context
import mdoc.internal.cli.MdocProperties
import mdoc.internal.cli.Settings
import mdoc.internal.livereload.TableOfContents
import mdoc.internal.livereload.UndertowLiveReload
import mdoc.internal.lsp.MdocEnrichments._
import mdoc.internal.markdown.Markdown
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.util.control.NonFatal

class MdocLanguageServer(
    ec: ExecutionContext
) {
  val sh = Executors.newSingleThreadScheduledExecutor()
  var workspace: AbsolutePath = _
  var client: MdocLanguageClient = _
  val contexts = TrieMap.empty[AbsolutePath, Context]
  var reporter: DiagnosticsReporter = _
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
    LspLogger.update(client)
  }

  def loadContexts(): Unit = {
    contexts.clear()
    val props = mdocPropertyCandidates()
    props.foreach { file =>
      try {
        loadContext(file)
      } catch {
        case NonFatal(e) =>
          scribe.error(s"mdoc.properties error: $file", e)
      }
    }
  }

  def loadContext(file: AbsolutePath): Unit = {
    val jprops = new Properties()
    val in = Files.newInputStream(file.toNIO)
    try jprops.load(in)
    finally in.close()
    val mprops = MdocProperties.fromProps(jprops, workspace)
    val settings = Settings.baseDefault(workspace).withProperties(mprops)
    val context = settings.validate(reporter).get
    contexts(settings.in) = context
  }

  def mdocPropertyCandidates(): List[AbsolutePath] = {
    val candidates = ListBuffer.empty[AbsolutePath]
    Files.walkFileTree(
      workspace.toNIO,
      new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (file.endsWith("mdoc.properties")) {
            candidates += AbsolutePath(file)
          }
          super.visitFile(file, attrs)
        }
      }
    )
    candidates.toList
  }

  def newSettings(): Settings = {
    val candidates = mdocPropertyCandidates()
    val base = Settings.default(workspace).copy(in = workspace, out = tmp)
    candidates match {
      case Nil =>
        base
      case head :: _ =>
        val in = Files.newInputStream(head.toNIO)
        val props = new java.util.Properties()
        try props.load(in)
        finally in.close()
        val mprops = MdocProperties.fromProps(props, workspace)
        base.withProperties(mprops)
    }
  }

  @JsonRequest("initialize")
  def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {
    CompletableFuture.completedFuture {
      val capabilities = new ServerCapabilities
      workspace = params.getRootUri.toAbsolutePath
      reporter = new DiagnosticsReporter(client)
      livereload = UndertowLiveReload(
        workspace.toNIO,
        reporter = reporter,
        lastPreview = () => currentPreview.get()
      )
      livereload.start()
      loadContexts()
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
      LspLogger.languageClient = None
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

  def previewMarkdown(uri: String, abspath: AbsolutePath, context: Context): Unit = {
    val input = uri.toInput
    scribe.info(s"Compiling ${input.path}")
    reporter.reset()
    client.status(MdocStatusParams(s"Compiling...", show = true))
    val relpath = abspath.toRelative(workspace)
    val markdown = Markdown.mdocSettings(context)
    markdown.set(Markdown.RelativePathKey, Some(relpath))
    markdown.set(Markdown.InputKey, Some(input))
    val document = Markdown.toDocument(input, markdown, reporter, context.settings)
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

  def findContext(path: AbsolutePath): Option[Context] = {
    for {
      (in, context) <- contexts
      if path.toNIO.startsWith(in.toNIO)
    } yield context
  }.headOption

  @JsonNotification("mdoc/preview")
  def preview(uri: String): Unit = {
    if (uri.endsWith(".md")) {
      val path = uri.toAbsolutePath
      findContext(path) match {
        case Some(context) =>
          previewMarkdown(uri, path, context)
        case None =>
          scribe.warn(s"no context: $uri")
      }
    }
  }

  @JsonNotification("textDocument/didSave")
  def didSave(params: DidSaveTextDocumentParams): Unit = {
    preview(params.getTextDocument.getUri)
  }
  @JsonNotification("textDocument/didClose")
  def didClose(params: DidCloseTextDocumentParams): Unit = ()
  @JsonNotification("workspace/didChangeConfiguration")
  def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = ()

  @JsonNotification("workspace/executeCommand")
  def executeCommand(params: ExecuteCommandParams): CompletableFuture[Object] = {
    params.getCommand match {
      case "reload" =>
        loadContexts()
        scribe.info(s"Loaded contexts: ${contexts.size}")
      case els =>
        scribe.warn(s"unknown command: '$els'")
    }
    CompletableFuture.completedFuture(new Object)
  }
}
