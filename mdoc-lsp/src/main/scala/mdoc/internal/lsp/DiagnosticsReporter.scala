package mdoc.internal.lsp

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.{util => jutil}
import mdoc.Reporter
import mdoc.internal.lsp.MdocEnrichments._
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.{lsp4j => l}
import scala.collection.JavaConverters._
import scala.meta.inputs.Position
import scala.meta.io.AbsolutePath

class DiagnosticsReporter(client: MdocLanguageClient) extends Reporter {
  val diagnostics = new ConcurrentHashMap[AbsolutePath, jutil.Queue[Diagnostic]]()
  val myErrors = new AtomicInteger()
  val myWarnings = new AtomicInteger()

  override def errorCount: Int = myErrors.get()
  override def warningCount: Int = myWarnings.get()
  def add(pos: Position, sev: DiagnosticSeverity, msg: String): Unit = {

    if (sev == DiagnosticSeverity.Error) {
      myErrors.incrementAndGet()
    } else if (sev == DiagnosticSeverity.Warning) {
      myWarnings.incrementAndGet()
    }

    val path = pos.input.syntax.toAbsolutePath
    val buf = diagnostics.computeIfAbsent(path, _ => new ConcurrentLinkedQueue[Diagnostic]())
    val diagnostic = new Diagnostic(
      new l.Range(
        new l.Position(pos.startLine, pos.startColumn),
        new l.Position(pos.endLine, pos.endColumn)
      ),
      msg,
      sev,
      "mdoc"
    )
    buf.add(diagnostic)
  }
  override def error(throwable: Throwable): Unit = {
    logMessage(MessageType.Error, throwable.formattedStackTrace)
  }
  override def error(pos: Position, throwable: Throwable): Unit = {
    add(pos, DiagnosticSeverity.Error, throwable.formattedStackTrace)
  }
  override def error(pos: Position, msg: String): Unit = {
    add(pos, DiagnosticSeverity.Error, msg)
  }
  override def error(msg: String): Unit = {
    logMessage(MessageType.Warning, msg)
  }
  override def warning(pos: Position, msg: String): Unit = {
    add(pos, DiagnosticSeverity.Warning, msg)
  }
  override def warning(msg: String): Unit = {
    logMessage(MessageType.Warning, msg)
  }
  override def info(pos: Position, msg: String): Unit = {
    add(pos, DiagnosticSeverity.Information, msg)
  }
  override def info(msg: String): Unit = logMessage(MessageType.Info, msg)
  override def print(msg: String): Unit = logMessage(MessageType.Log, msg)
  override def println(msg: String): Unit = print(msg)
  private def logMessage(tpe: MessageType, msg: String): Unit = {
    client.logMessage(new MessageParams(MessageType.Log, msg))
  }
  override private[mdoc] def hasWarnings: Boolean = myWarnings.get() > 0
  override private[mdoc] def hasErrors: Boolean = myErrors.get() > 0
  override private[mdoc] def reset(): Unit = {
    myErrors.set(0)
    myWarnings.set(0)
    diagnostics.clear()
  }
  def publishDiagnostics(path: AbsolutePath): Unit = {
    diagnostics.putIfAbsent(path, new ConcurrentLinkedQueue[Diagnostic]())
    diagnostics.asScala.foreach {
      case (path, diags) =>
        val uri = path.toURI.toString
        val lst = new jutil.ArrayList(diags)
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, lst))
    }
  }
}
