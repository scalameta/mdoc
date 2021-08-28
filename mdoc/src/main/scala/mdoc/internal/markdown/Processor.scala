package mdoc.internal.markdown

import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.util.ast
import com.vladsch.flexmark.parser.block.DocumentPostProcessor
import com.vladsch.flexmark.parser.block.DocumentPostProcessorFactory
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.CharSubSequence
import scala.meta._
import java.util
import mdoc.PostModifierContext
import mdoc.PostProcessContext
import mdoc.PreModifierContext
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.dialects.{Scala213, Scala3}
import scala.util.control.NonFatal
import mdoc.internal.cli.Context
import mdoc.internal.document.MdocExceptions
import mdoc.internal.markdown.Modifier._
import mdoc.internal.pos.PositionSyntax._
import pprint.TPrintColors
import scala.meta.io.RelativePath
import coursierapi.error.SimpleResolutionError
import coursierapi.error.CoursierError
import coursierapi.error.MultipleResolutionError
import scala.meta.io.AbsolutePath
import scala.meta.parsers.Parsed
import scala.meta.Source
import mdoc.internal.BuildInfo

object MdocDialect {

  def parse(path: AbsolutePath): Parsed[Source] = {
    (Input.VirtualFile(path.toString, path.readText), scala).parse[Source]
  }

  val scala =
    if (BuildInfo.scalaBinaryVersion.startsWith("3"))
      Scala3.withAllowToplevelTerms(true)
    else Scala213.withAllowToplevelTerms(true)
}

class Processor(implicit ctx: Context) {

  def processDocument(doc: MarkdownFile): MarkdownFile = {
    val docInput = doc.input
    val (scalaInputs, customInputs, preInputs) = collectFenceInputs(doc)
    val filename = docInput.toFilename(ctx.settings)
    val inputFile = doc.file.relpath
    customInputs.foreach { block => processStringInput(doc, block) }
    preInputs.foreach { block => processPreInput(doc, block) }
    if (scalaInputs.nonEmpty) {
      processScalaInputs(doc, scalaInputs, inputFile, filename)
    }
    if (preInputs.nonEmpty) {
      val post = new PostProcessContext(ctx.reporter, doc.file, ctx.settings)
      ctx.settings.preModifiers.foreach { pre =>
        runModifier(
          pre.name,
          () => {
            appendChild(doc, pre.postProcess(post))
          }
        )
      }
    }
    doc
  }

  def runModifier(name: String, fn: () => Unit): Unit = {
    try {
      fn()
    } catch {
      case NonFatal(e) =>
        ctx.reporter.error(new ModifierException(name, e))
    }
  }

  def processPreInput(doc: MarkdownFile, custom: PreFenceInput): Unit = {
    val PreFenceInput(block, input, Pre(mod, info)) = custom
    try {
      val inputFile = doc.file.relpath
      val preCtx = new PreModifierContext(
        info,
        input,
        ctx.reporter,
        doc.file,
        ctx.settings
      )
      val out = mod.process(preCtx)
      replaceNodeWithText(doc, block, out)
    } catch {
      case NonFatal(e) =>
        val pos = Position.Range(input, 0, input.chars.length)
        MdocExceptions.trimStacktrace(e)
        val exception = new ModifierException(mod.name, e)
        ctx.reporter.error(pos, exception)
    }
  }

  def processStringInput(doc: MarkdownFile, custom: StringFenceInput): Unit = {
    val StringFenceInput(block, input, Str(mod, info)) = custom
    try {
      val newText = mod.process(info, input, ctx.reporter)
      replaceNodeWithText(doc, block, newText)
    } catch {
      case NonFatal(e) =>
        val pos = Position.Range(input, 0, input.chars.length)
        MdocExceptions.trimStacktrace(e)
        val exception = new ModifierException(mod.name, e)
        ctx.reporter.error(pos, exception)
    }
  }

  def processScalaInputs(
      doc: MarkdownFile,
      inputs: List[ScalaFenceInput],
      relpath: RelativePath,
      filename: String
  ): Unit = {
    val sectionInputs = inputs.map { case ScalaFenceInput(_, input, mod) =>
      import scala.meta._

      (input, MdocDialect.scala).parse[Source] match {
        case parsers.Parsed.Success(source) =>
          SectionInput(input, ParsedSource(source), mod)
        case parsers.Parsed.Error(pos, msg, _) =>
          ctx.reporter.error(pos.toUnslicedPosition, msg)
          SectionInput(input, ParsedSource.empty, mod)
      }
    }
    val instrumented = Instrumenter.instrument(doc.file, sectionInputs, ctx.settings, ctx.reporter)

    if (ctx.reporter.hasErrors) {
      return
    }
    if (ctx.settings.verbose) {
      ctx.reporter.info(s"Instrumented $filename")
      ctx.reporter.println(instrumented.source)
    }
    val compiler =
      try {
        ctx.compiler(instrumented)
      } catch {
        case e: CoursierError =>
          handleCoursierError(instrumented, e)
          ctx.compiler
      }
    processScalaInputs(
      doc,
      inputs,
      relpath,
      filename,
      sectionInputs,
      instrumented,
      compiler
    )
  }
  def handleCoursierError(instrumented: Instrumented, e: CoursierError): Unit = {
    e match {
      case m: MultipleResolutionError =>
        m.getErrors().asScala.foreach(simpleError => handleCoursierError(instrumented, simpleError))
      case _ =>
        val pos = instrumented.positionedDependencies
          .collectFirst {
            case dep if e.getMessage().contains(dep.syntax) =>
              dep.pos
          }
          .orElse(instrumented.dependencyImports.headOption.map(_.pos))
          .getOrElse(Position.None)
        ctx.reporter.error(pos, e.getMessage())
    }
  }

  def processScalaInputs(
      doc: MarkdownFile,
      inputs: List[ScalaFenceInput],
      relpath: RelativePath,
      filename: String,
      sectionInputs: List[SectionInput],
      instrumented: Instrumented,
      markdownCompiler: MarkdownCompiler
  ): Unit = {
    // TODO Possibly hook in here?
    val rendered = MarkdownBuilder.buildDocument(
      markdownCompiler,
      ctx.reporter,
      sectionInputs,
      instrumented,
      filename
    )
    rendered.sections.zip(inputs).foreach { case (section, ScalaFenceInput(block, _, mod)) =>
      block.newInfo = Some("scala")
      def defaultRender: String =
        Renderer.renderEvaluatedSection(
          rendered,
          section,
          ctx.reporter,
          ctx.settings.variablePrinter,
          markdownCompiler
        )
      implicit val pprintColor = TPrintColors.BlackWhite
      mod match {
        case Modifier.Post(modifier, info) =>
          val variables = for {
            (stat, i) <- section.section.statements.zipWithIndex
            n = section.section.statements.length
            (binder, j) <- stat.binders.zipWithIndex
            m = stat.binders.length
          } yield {
            new mdoc.Variable(
              binder.name,
              binder.tpe.render,
              binder.value,
              binder.pos.toMeta(section),
              j,
              n,
              i,
              m,
              section.mod
            )
          }
          val postCtx = new PostModifierContext(
            info,
            section.input,
            defaultRender,
            variables,
            ctx.reporter,
            doc.file,
            ctx.settings
          )
          val postRender = modifier.process(postCtx)
          replaceNodeWithText(doc, block, postRender)
        case m: Modifier.Builtin =>
          if (m.isPassthrough) {
            replaceNodeWithText(doc, block, section.out)
          } else if (m.isInvisible) {
            replaceNodeWithText(doc, block, "")
          } else if (m.isCrash) {
            val stacktrace =
              Renderer.renderCrashSection(section, ctx.reporter, rendered.edit)
            replaceNodeWithText(doc, block, stacktrace)
          } else if (m.isSilent) {
            () // Do nothing
          } else {
            block.newBody = Some(defaultRender)
          }
        case c: Modifier.Str =>
          throw new IllegalArgumentException(c.toString)
        case c: Modifier.Pre =>
          val preCtx = new PreModifierContext(
            c.info,
            section.input,
            ctx.reporter,
            doc.file,
            ctx.settings
          )
          val out =
            try c.mod.process(preCtx)
            catch {
              case NonFatal(e) =>
                ctx.reporter.error(e)
                e.getMessage
            }
          replaceNodeWithText(doc, block, out)
      }
    }
  }

  def appendChild(doc: MarkdownFile, text: String): Unit = {
    if (text.nonEmpty) {
      doc.appendText(text)
    }
  }

  def replaceNodeWithText(doc: MarkdownFile, toReplace: CodeFence, text: String): Unit = {
    toReplace.newPart = Some(text)
  }

  def collectFenceInputs(
      doc: MarkdownFile
  ): (List[ScalaFenceInput], List[StringFenceInput], List[PreFenceInput]) = {
    val InterestingCodeFence = new FenceInput(ctx, doc.input)
    val inputs = List.newBuilder[ScalaFenceInput]
    val strings = List.newBuilder[StringFenceInput]
    val pres = List.newBuilder[PreFenceInput]
    doc.parts.foreach {
      case InterestingCodeFence(input) =>
        input.mod match {
          case string: Str =>
            strings += StringFenceInput(input.block, input.input, string)
          case pre: Pre =>
            pres += PreFenceInput(input.block, input.input, pre)
          case _ =>
            inputs += input
        }
      case _ =>
    }
    (inputs.result(), strings.result(), pres.result())
  }
}
