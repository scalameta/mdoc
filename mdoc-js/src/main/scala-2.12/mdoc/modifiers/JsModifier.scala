package mdoc.modifiers

import mdoc.{OnLoadContext, PostProcessContext, PreModifierContext}
import mdoc.internal.cli.InputFile
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.livereload.Resources
import mdoc.internal.markdown.{CodeBuilder, Gensym, MarkdownCompiler}
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance
import org.scalajs.linker.interface.{IRContainer, Linker, Semantics}
import org.scalajs.linker.standard.StandardIRFileCache
import org.scalajs.logging.{Level, Logger}

import scala.collection.mutable.ListBuffer
import scala.meta.Term
import scala.meta.inputs.Input
import scala.meta.io.{AbsolutePath, Classpath}
import scala.reflect.io.VirtualDirectory
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.StandardImpl
import org.scalajs.linker.PathIRContainer
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.scalajs.linker.interface.IRFile
import org.scalajs.linker.PathIRFile
import org.scalajs.linker.standard.MemIRFileImpl
import org.scalajs.linker.interface.LinkerOutput
import org.scalajs.linker.MemOutputFile
import java.util.concurrent.Executor

class JsModifier extends mdoc.PreModifier {
  override val name = "js"
  override def toString: String = s"JsModifier($config)"
  val irCache = new StandardIRFileCache
  val target = new VirtualDirectory("(memory)", None)
  var maybeCompiler: Option[MarkdownCompiler] = None
  var config = JsConfig()
  var linker: Linker = newLinker()
  var virtualIrFiles: Seq[IRFile] = Nil
  var classpathHash: Int = 0
  var reporter: mdoc.Reporter = new ConsoleReporter(System.out)
  var gensym = new Gensym()

  implicit val synchronousExecutionContext = ExecutionContext.fromExecutor(new Executor {
    def execute(task: Runnable) = task.run()
  })

  val sjsLogger: Logger = new Logger {
    override def log(level: Level, message: => String): Unit = {
      if (level >= config.minLevel) {
        if (level == Level.Warn) reporter.info(message)
        else if (level == Level.Error) reporter.info(message)
        else reporter.info(message)
      }
    }

    override def trace(t: => Throwable): Unit =
      reporter.error(t)
  }

  private val runs = ListBuffer.empty[String]
  private val inputs = ListBuffer.empty[Input]

  def reset(): (List[String], List[Input]) = {
    val result = (runs.toList, inputs.toList)
    runs.clear()
    inputs.clear()
    gensym.reset()
    result
  }

  def newLinker(): Linker = {
    val semantics: Semantics =
      if (config.fullOpt) Semantics.Defaults.optimized
      else Semantics.Defaults

      val conf = StandardConfig()
        .withSemantics(semantics)
        .withSourceMap(false)
        .withModuleKind(config.moduleKind)
        .withClosureCompilerIfAvailable(config.fullOpt)

    StandardImpl.linker(conf)
  }

  override def onLoad(ctx: OnLoadContext): Unit = {
    (ctx.site.get("js-classpath"), ctx.site.get("js-scalac-options")) match {
      case (None, None) => // nothing to do
      case (Some(_), None) =>
        ctx.reporter.error("missing key: 'js-scalac-options'")
      case (None, Some(_)) =>
        ctx.reporter.error("missing key: 'js-classpath'")
      case (Some(classpath), Some(scalacOptions)) =>
        config = JsConfig.fromVariables(ctx)
        reporter = ctx.reporter
        val newClasspathHash = (classpath, scalacOptions, config.fullOpt).hashCode()
        // Reuse the  linker and compiler when the classpath+scalacOptions haven't changed
        // to speed up unit tests by nearly 2x.
        if (classpathHash != newClasspathHash) {
          linker = newLinker()
          classpathHash = newClasspathHash
          maybeCompiler = Some(new MarkdownCompiler(classpath, scalacOptions, target))
          virtualIrFiles = {

            val irContainer =
              PathIRContainer.fromClasspath(Classpath(classpath).entries.map(_.toNIO))

            val cache = irCache.newCache

            Await.result(
              irContainer.map(_._1).flatMap(fs => cache.cached(fs)), 
              Duration.Inf
            )
          }
        }
    }
  }

  override def postProcess(ctx: PostProcessContext): String = {
    if (runs.isEmpty) {
      reset()
      ""
    } else {
      maybeCompiler match {
        case None =>
          ctx.reporter.error(
            inputs.head.toPosition,
            "Can't process `mdoc:js` code fence because Scala.js is not configured. " +
              "To fix this problem, set the site variables `js-classpath` and `js-scalac-options`. " +
              "If you are using sbt-mdoc, update the `mdocJS` setting to point to a Scala.js project."
          )
          ""
        case Some(compiler) =>
          postProcess(ctx, compiler)
      }
    }
  }

  def postProcess(ctx: PostProcessContext, compiler: MarkdownCompiler): String = {
    val (runs, inputs) = reset()
    val code = new CodeBuilder()
    val wrapped = code
      .println("object mdocjs {")
      .foreach(runs)(code.println)
      .println("}")
      .toString
    val input = Input.VirtualFile(ctx.relativePath.toString(), wrapped)
    val edit = TokenEditDistance.fromInputs(inputs, input)
    val oldErrors = ctx.reporter.errorCount
    compiler.compileSources(input, ctx.reporter, edit, fileImports = Nil)
    val hasErrors = ctx.reporter.errorCount > oldErrors
    val sjsir = for {
      x <- target.toList
      if x.name.endsWith(".sjsir")
    } yield {
      new MemIRFileImpl(
        x.path,
        None,
        x.toByteArray
      ): IRFile
    }
    if (sjsir.isEmpty) {
      if (!hasErrors) {
        ctx.reporter.error("Scala.js compilation failed")
      }
      ""
    } else {
      val output = MemOutputFile.apply()
      linker.link(virtualIrFiles ++ sjsir, Nil, LinkerOutput.apply(output), sjsLogger)
      ctx.settings.toInputFile(ctx.inputFile) match {
        case None =>
          ctx.reporter.error(
            s"unable to find output file matching the input file '${ctx.inputFile}'. " +
              s"To fix this problem, make sure that  --in points to a directory that contains the file ${ctx.inputFile}."
          )
          ""
        case Some(inputFile) =>
          val outjsfile = resolveOutputJsFile(inputFile)
          outjsfile.write(new String(output.content))
          val outmdoc = outjsfile.resolveSibling(_ => "mdoc.js")
          outmdoc.write(Resources.readPath("/mdoc.js"))
          val relfile = outjsfile.toRelativeLinkFrom(ctx.outputFile, config.relativeLinkPrefix)
          val relmdoc = outmdoc.toRelativeLinkFrom(ctx.outputFile, config.relativeLinkPrefix)
          new CodeBuilder()
            .println(config.htmlHeader)
            .lines(config.libraryScripts(outjsfile, ctx))
            .println(s"""<script type="text/javascript" src="$relfile" defer></script>""")
            .println(s"""<script type="text/javascript" src="$relmdoc" defer></script>""")
            .toString
      }
    }
  }

  private def resolveOutputJsFile(file: InputFile): AbsolutePath = {
    val outputDirectory = config.outPrefix match {
      case None =>
        file.outputDirectory
      case Some(prefix) =>
        // This is needed for Docusaurus that requires assets (non markdown) files to live under
        // `docs/assets/`: https://docusaurus.io/docs/en/doc-markdown#linking-to-images-and-other-assets
        file.outputDirectory.resolve(prefix)
    }
    outputDirectory.resolve(file.relpath).resolveSibling(_ + ".js")
  }

  override def process(ctx: PreModifierContext): String = {
    JsMods.parse(ctx.infoInput, ctx.reporter) match {
      case Some(mods) =>
        process(ctx, mods)
      case None =>
        ""
    }
  }

  def process(ctx: PreModifierContext, mods: JsMods): String = {
    val separator = "\n---\n"
    val text = ctx.originalCode.text
    val separatorIndex = text.indexOf(separator)
    val (body, input) =
      if (separatorIndex < 0) {
        ("", ctx.originalCode)
      } else {
        val sliced = Input.Slice(
          ctx.originalCode,
          separatorIndex + separator.length,
          ctx.originalCode.chars.length
        )
        (
          text.substring(0, separatorIndex),
          sliced
        )
      }
    val run = gensym.fresh("run")
    inputs += input
    val htmlId = s"mdoc-html-$run"
    val jsId = s"mdoc_js_$run"
    val mountNodeParam = Term.Name(config.mountNode)
    val code: String =
      if (mods.isShared) {
        input.text
      } else if (mods.isCompileOnly) {
        new CodeBuilder()
          .println(s"""object ${gensym.fresh("compile")} {""")
          .println(input.text)
          .println("}")
          .toString
      } else {
        new CodeBuilder()
          .println(s"""@_root_.scala.scalajs.js.annotation.JSExportTopLevel("$jsId") """)
          .println(
            s"""def $run($mountNodeParam: _root_.org.scalajs.dom.raw.HTMLElement): Unit = {"""
          )
          .println(input.text)
          .println("}")
          .toString
      }

    runs += code
    new CodeBuilder()
      .printlnIf(!mods.isInvisible, s"```scala\n${input.text}\n```")
      .printlnIf(mods.isEntrypoint, s"""<div id="$htmlId" data-mdoc-js>$body</div>""")
      .toString
  }
}
