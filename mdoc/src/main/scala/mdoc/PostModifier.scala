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

/**
  * Interface of classes used for processing Markdown code fences.
  * It provides method calls to set-up resources before processing
  * sources, process code fences of all source files and release
  * resource just before mdoc terminates.
  *
  */
trait PostModifier {
  val name: String

  /**
    * This methods is called once just before mdoc starts processing all of the
    * source files. Use this to set-up resources required by the post-modifier.
    *
    * @param settings setting set via the command line or directly vi the API
    */
  def onStart(settings: MainSettings): Unit = ()
  def process(ctx: PostModifierContext): String

  /**
    * This methods is called once just after mdoc finished processing all of the
    * source files. Use this to release or deactivate any resources that are not
    * required by the post-modifier anymore.
    *
    * @param exit a value of 0 indicates mdoc processed all files with no error.
    *             a value of 1 indicates mdoc processing resulted in at least
    *             one error.
    */
  def onExit(exit: Exit): Unit = ()
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
