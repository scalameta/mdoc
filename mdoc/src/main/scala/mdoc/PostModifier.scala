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

trait PostModifier {
  val name: String
  def onStart(settings: MainSettings): Unit = ()
  def preProcess(ctx: PostModifierContext): Unit = ()
  def process(ctx: PostModifierContext): String
  def postProcess(ctx: PostModifierContext): Unit = ()
  def onExit(exit: Int): Unit = ()
}

object PostModifier {
  def default(): List[PostModifier] = default(this.getClass.getClassLoader)
  def default(classLoader: ClassLoader): List[PostModifier] =
    ServiceLoader.load(classOf[PostModifier], classLoader).iterator().asScala.toList
  implicit val surface: Surface[Settings] = new Surface(Nil)
  implicit val decoder: ConfDecoder[PostModifier] =
    ConfDecoder.instanceF[PostModifier](_ => ConfError.message("unsupported").notOk)
  implicit val encoder: ConfEncoder[PostModifier] =
    ConfEncoder.StringEncoder.contramap(mod => s"<${mod.name}>")
}

final class PostModifierContext private[mdoc] (
    val info: String,
    val originalCode: Input,
    val outputCode: String,
    val variables: List[Variable],
    val reporter: Reporter,
    val relativePath: RelativePath,
    private[mdoc] val settings: Settings
) {
  def inputFile: AbsolutePath = inDirectory.resolve(relativePath)
  def outputFile: AbsolutePath = outDirectory.resolve(relativePath)
  def lastValue: Any = variables.lastOption.map(_.runtimeValue).orNull
  def inDirectory: AbsolutePath = settings.in
  def outDirectory: AbsolutePath = settings.out
}
