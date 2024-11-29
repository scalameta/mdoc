package mdoc

import java.util.ServiceLoader
import mdoc.internal.cli.Settings
import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.generic.Surface
import scala.meta.inputs.Input
import scala.meta.io.AbsolutePath
import scala.collection.JavaConverters._
import scala.meta.io.RelativePath
import mdoc.internal.cli.InputFile

trait PostModifier {
  val name: String
  def process(ctx: PostModifierContext): String
}

object PostModifier {
  def default(): List[PostModifier] = default(this.getClass.getClassLoader)
  def default(classLoader: ClassLoader): List[PostModifier] =
    ServiceLoader.load(classOf[PostModifier], classLoader).iterator().asScala.toList
  implicit val surface: Surface[Settings] = new Surface(Nil)
  implicit val decoder: ConfDecoder[PostModifier] =
    ConfDecoder.from[PostModifier](_ => ConfError.message("unsupported").notOk)
  implicit val encoder: ConfEncoder[PostModifier] =
    ConfEncoder.StringEncoder.contramap(mod => s"<${mod.name}>")
}

final class PostModifierContext private[mdoc] (
    val info: String,
    val originalCode: Input,
    val outputCode: String,
    val variables: List[Variable],
    val reporter: Reporter,
    private[mdoc] val file: InputFile,
    private[mdoc] val settings: Settings
) {
  def lastValue: Any = variables.lastOption.map(_.runtimeValue).orNull
  def relativePath: RelativePath = file.relpath
  def inputFile: AbsolutePath = file.inputFile
  def outputFile: AbsolutePath = file.outputFile
  def inDirectory: AbsolutePath = file.inputDirectory
  def outDirectory: AbsolutePath = file.outputDirectory
}
