package mdoc.modifiers

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

object ImportMapJsonIr {

  type Scope = Map[String, String]

  final case class ImportMap(
      val imports: Map[String, String],
      val scopes: Option[Map[String, Scope]]
  )

  object ImportMap {
    implicit val codec: JsonValueCodec[ImportMap] = JsonCodecMaker.make
  }

}
