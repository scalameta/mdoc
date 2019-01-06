package mdoc

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

package object docs {
  implicit lazy val executor: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(null, _ => ())
}
