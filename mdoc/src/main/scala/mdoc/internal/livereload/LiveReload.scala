package mdoc.internal.livereload

import java.nio.file.Path

/** Interface of a LiveReload server http://livereload.com/api/protocol/
  */
trait LiveReload {

  /** Start this LiveReload server */
  def start(): Unit

  /** Stop this LiveReload server */
  def stop(): Unit

  /** Notify clients to reload this path */
  def reload(path: Path): Unit

  /** Alert clients with this message */
  def alert(message: String): Unit
}
