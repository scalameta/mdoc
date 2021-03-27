package mdoc.internal.cli

import java.nio.file.PathMatcher
import pprint._
import scala.meta.io._

trait MetaconfigScalametaImplicits {
  implicit val absolutePathPrint: TPrint[AbsolutePath] =
    TPrint.make[AbsolutePath](_ => "<path>")
  implicit val pathMatcherPrint: TPrint[PathMatcher] =
    TPrint.make[PathMatcher](_ => "<glob>")
  implicit def optionPrint[T](implicit ev: pprint.TPrint[T]): TPrint[Option[T]] =
    TPrint.make { implicit cfg => ev.render }
  implicit def iterablePrint[C[x] <: Iterable[x], T](implicit ev: pprint.TPrint[T]): TPrint[C[T]] =
    TPrint.make { implicit cfg => s"[${ev.render} ...]" }
}

object MetaconfigScalametaImplicits extends MetaconfigScalametaImplicits
