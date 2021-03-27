package mdoc.internal.cli

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.PathMatcher
import mdoc.OnLoadContext
import mdoc.PostModifier
import mdoc.PreModifier
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.annotation._
import metaconfig.generic
import metaconfig.generic.Surface
import org.typelevel.paiges.Doc
import scala.annotation.StaticAnnotation
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import mdoc.StringModifier
import mdoc.Variable
import mdoc.Reporter
import mdoc.internal.markdown.{GitHubIdGenerator, ReplVariablePrinter}
import mdoc.internal.cli.CliEnrichments._
trait Decoders {

  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob => FileSystems.getDefault.getPathMatcher("glob:" + glob))
  implicit val LoggerEncoder: ConfEncoder[coursierapi.Logger] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val LoggerDecoder: ConfDecoder[coursierapi.Logger] =
    ConfDecoder.stringConfDecoder.flatMap {
      case "nop" => Configured.ok(coursierapi.Logger.nop())
      case "progress-bars" => Configured.ok(coursierapi.Logger.progressBars())
      case unknown =>
        Configured.error(
          s"unknown coursier logger '$unknown'. Expected one of 'nop' or 'progress-bars'"
        )
    }
  implicit val CharsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.flatMap { str =>
      try {
        Configured.ok(Charset.forName(str))
      } catch {
        case _: UnsupportedCharsetException =>
          ConfError.message(s"Charset '$str' is unsupported").notOk
        case _: IllegalCharsetNameException =>
          ConfError.message(s"Charset name '$str' is illegal").notOk
      }
    }
  implicit val inputStreamDecoder: ConfDecoder[InputStream] =
    ConfDecoder.stringConfDecoder.map(_ => System.in)
  implicit val headerIdGeneratorDecoder: ConfDecoder[String => String] =
    ConfDecoder.stringConfDecoder.flatMap(_ => ConfError.message("unsupported").notOk)
  implicit val variablePrinterDecoder: ConfDecoder[Variable => String] =
    ConfDecoder.stringConfDecoder.flatMap(_ => ConfError.message("unsupported").notOk)

  implicit val pathEncoder: ConfEncoder[AbsolutePath] =
    ConfEncoder.StringEncoder.contramap { path =>
      if (path == PathIO.workingDirectory) "<current working directory>"
      else path.toRelative(PathIO.workingDirectory).toString()
    }
  implicit val pathMatcherEncoder: ConfEncoder[PathMatcher] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val charsetEncoder: ConfEncoder[Charset] =
    ConfEncoder.StringEncoder.contramap(_.name())
  implicit val inputStreamEncoder: ConfEncoder[InputStream] =
    ConfEncoder.StringEncoder.contramap(_ => "<input stream>")
  implicit val headerIdGeneratorEncoder: ConfEncoder[String => String] =
    ConfEncoder.StringEncoder.contramap(_ => "<String => String>")
  implicit val variablePrinterEncoder: ConfEncoder[Variable => String] =
    ConfEncoder.StringEncoder.contramap(_ => "<Variable => String>")

}

object Decoders extends Decoders
