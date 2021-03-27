package mdoc.internal.cli

import metaconfig.generic.Surface
import metaconfig.generic
import metaconfig.ConfEncoder
import metaconfig.ConfDecoder
import scala.meta.io.AbsolutePath
import java.nio.file.InvalidPathException
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Conf
import scala.meta.internal.io.PathIO

trait SettingsGeneric { self: MetaconfigScalametaImplicits with Decoders =>

  implicit val surface: Surface[Settings] =
    generic.deriveSurface[Settings]
  implicit val encoder: ConfEncoder[Settings] =
    generic.deriveEncoder[Settings]
  def decoder(base: Settings): ConfDecoder[Settings] = {
    implicit val cwd = base.cwd
    implicit val PathDecoder: ConfDecoder[AbsolutePath] =
      ConfDecoder.stringConfDecoder.flatMap { str =>
        try {
          Configured.ok(AbsolutePath(str))
        } catch {
          case e: InvalidPathException =>
            ConfError.message(e.getMessage).notOk
        }
      }
    generic.deriveDecoder[Settings](base)
  }

  def fromCliArgs(args: List[String], base: Settings): Configured[Settings] = {
    Conf
      .parseCliArgs[Settings](args)
      .andThen(conf => {
        val cwd = conf.get[String]("cwd").map(AbsolutePath(_)(base.cwd)).getOrElse(base.cwd)
        conf.as[Settings](decoder(base.copy(cwd = cwd)))
      })
      .map(_.addSite(base.site))
  }

  def write(set: Settings) = ConfEncoder[Settings].write(set)

}
