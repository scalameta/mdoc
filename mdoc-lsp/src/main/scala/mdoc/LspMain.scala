package mdoc

import java.io.PrintStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import mdoc.internal.lsp.MdocLanguageClient
import mdoc.internal.lsp.MdocLanguageServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import scala.concurrent.ExecutionContext
import scala.meta.internal.io.PathIO
import scala.util.control.NonFatal

object LspMain {
  def main(args: Array[String]): Unit = {
    val systemIn = System.in
    val systemOut = System.out
    val cwd = PathIO.workingDirectory.resolve(".mdoc")
    Files.createDirectories(cwd.toNIO)
    val log = cwd.resolve("mdoc.log")
    val out = Files.newOutputStream(log.toNIO, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    val ps = new PrintStream(out)
    val trace = Files.newOutputStream(
      cwd.resolve("mdoc.trace.json").toNIO,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
    System.setErr(ps)
    System.setOut(ps)
    val exec = Executors.newCachedThreadPool()
    val server = new MdocLanguageServer(ExecutionContext.fromExecutor(exec))
    try {
      val launcher = new Launcher.Builder[MdocLanguageClient]()
        .traceMessages(new PrintWriter(trace))
        .setExecutorService(exec)
        .setInput(systemIn)
        .setOutput(systemOut)
        .setRemoteInterface(classOf[MdocLanguageClient])
        .setLocalService(server)
        .create()
      val client = launcher.getRemoteProxy
      server.connect(client)
      launcher.startListening().get()
    } catch {
      case NonFatal(e) =>
        e.printStackTrace(ps)
        sys.exit(1)
    } finally {
      exec.shutdown()
    }
  }
}
