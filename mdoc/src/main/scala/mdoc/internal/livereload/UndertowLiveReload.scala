package mdoc.internal.livereload

import io.undertow.Handlers.path
import io.undertow.Handlers.resource
import io.undertow.Handlers.websocket
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.PathResourceManager
import io.undertow.util.Headers
import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.StreamSourceFrameChannel
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.core.WebSockets
import io.undertow.websockets.spi.WebSocketHttpExchange
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import mdoc.Reporter
import mdoc.internal.io.ConsoleReporter
import scala.collection.mutable
import scala.meta.internal.io.InputStreamIO

final class UndertowLiveReload private (
    server: Undertow,
    reporter: Reporter,
    url: String,
    openChannels: mutable.Set[WebSocketChannel]
) extends LiveReload {
  override def start(): Unit = {
    reporter.info(s"LiveReload server started on $url/")
    server.start()
  }
  override def stop(): Unit = server.stop()
  override def reload(path: Path): Unit = {
    sendJson(s"""{"command":"reload","path":"$path","liveCss":true}""")
  }
  override def alert(message: String): Unit = {
    sendJson(s"""{"command":"alert","message":"$message"}""")
  }
  private def sendJson(json: String): Unit = {
    openChannels.foreach(channel => WebSockets.sendTextBlocking(json, channel))

  }
}

object UndertowLiveReload {

  /**
    * Instantiate an undertow file server that speaks the LiveReload protocol.
    *
    * See LiveReload protocol for more details: http://livereload.com/api/protocol/
    *
    * @param root the root directory to serve files from.
    * @param host the hostname of the server.
    * @param preferredPort the preferred port of the server. If the port is not free,
    *                      the first free port that is an increment of this port is picked.
    *                      For example, if preferredPort == 4000 and 4000 is not free, then
    *                      4001 will be picked instead.
    * @param reporter the reporter to use for logging purposes.
    */
  def apply(
      root: Path,
      host: String = "localhost",
      preferredPort: Int = 4000,
      reporter: Reporter = ConsoleReporter.default
  ): LiveReload = {
    val port = freePort(host, preferredPort)
    val url = s"http://$host:$port"
    val openChannels = mutable.Set.empty[WebSocketChannel]
    val fromFileSystem = resource(new PathResourceManager(root)).setDirectoryListingEnabled(true)
    val baseHandler =
      path()
        .addExactPath("/livereload.js", staticResource("/livereload.js"))
        .addExactPath("/highlight.js", staticResource("/highlight.js"))
        .addExactPath("/github.css", staticResource("/github.css"))
        .addExactPath("/custom.css", staticResource("/custom.css"))
        .addPrefixPath(
          "/livereload",
          websocket(new LiveReloadConnectionCallback(openChannels))
        )
        .addPrefixPath("/", fromFileSystem)
    val markdownHandler = new HttpHandler {
      override def handleRequest(exchange: HttpServerExchange): Unit = {
        if (exchange.getRequestPath.endsWith(".md")) {
          val in = root.resolve(exchange.getRequestPath.stripPrefix("/"))
          val markdown = new String(Files.readAllBytes(in), StandardCharsets.UTF_8)
          val html = SimpleHtml.fromMarkdown(markdown, in.getFileName.toString, url)
          exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/html")
          exchange.getResponseSender.send(html)
        } else {
          baseHandler.handleRequest(exchange)
        }
      }
    }
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(markdownHandler)
      .build()
    new UndertowLiveReload(server, reporter, url, openChannels)
  }

  private def staticResource(path: String): HttpHandler = {
    val is = this.getClass.getResourceAsStream(path)
    val bytes =
      try InputStreamIO.readBytes(is)
      finally is.close()
    val text = new String(bytes, StandardCharsets.UTF_8)
    new HttpHandler {
      override def handleRequest(exchange: HttpServerExchange): Unit = {
        exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, contentType(path))
        exchange.getResponseSender.send(text)
      }
    }
  }

  private def contentType(path: String): String = {
    if (path.endsWith(".js")) "application/javascript"
    else if (path.endsWith(".css")) "text/css"
    else if (path.endsWith(".html")) "text/html"
    else ""
  }

  private final class LiveReloadConnectionCallback(openChannels: mutable.Set[WebSocketChannel])
      extends WebSocketConnectionCallback {
    override def onConnect(
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel
    ): Unit = {
      channel.getReceiveSetter.set(new AbstractReceiveListener() {
        override def onClose(
            webSocketChannel: WebSocketChannel,
            channel: StreamSourceFrameChannel
        ): Unit = {
          openChannels.remove(webSocketChannel)
          super.onClose(webSocketChannel, channel)
        }
        override protected def onFullTextMessage(
            channel: WebSocketChannel,
            message: BufferedTextMessage
        ): Unit = {
          if (message.getData.contains("""command":"hello""")) {
            val hello =
              """{"command":"hello","protocols":["http://livereload.com/protocols/official-7"],"serverName":"mdoc"}"""
            WebSockets.sendTextBlocking(hello, channel)
            openChannels.add(channel)
          }
        }
      })
      channel.resumeReceives()
    }
  }

  private final def freePort(host: String, port: Int, maxRetries: Int = 20): Int = {
    try {
      val socket = new ServerSocket()
      try {
        socket.bind(new InetSocketAddress(host, port))
        val free = socket.getLocalPort
        free
      } finally {
        socket.close()
      }
    } catch {
      case _: IOException if maxRetries > 0 =>
        freePort(host, port + 1, maxRetries - 1)
    }
  }
}
