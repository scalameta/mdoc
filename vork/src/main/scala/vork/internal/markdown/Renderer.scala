package vork.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.inputs.Position
import vork.Reporter
import vork.document.CompileResult
import vork.document.CrashResult
import vork.document.CrashResult.Crashed
import vork.internal.document.VorkExceptions
import vork.internal.pos.PositionSyntax._
import vork.internal.pos.TokenEditDistance

object Renderer {

  def render(
      sections: List[Input],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String
  ): String = {
    val inputs =
      sections.map(s => SectionInput(s, dialects.Sbt1(s).parse[Source].get, Modifier.Default))
    val instrumented = Instrumenter.instrument(inputs)
    val doc =
      MarkdownCompiler.buildDocument(compiler, reporter, inputs, instrumented, filename)
    doc.sections
      .map(s => s"""```scala
                   |${Renderer.renderEvaluatedSection(doc, s, reporter)}
                   |```""".stripMargin)
      .mkString("\n")
  }

  def renderCrashSection(
      section: EvaluatedSection,
      reporter: Reporter,
      edit: TokenEditDistance
  ): String = {
    require(section.mod.isCrash, section.mod)
    val out = new ByteArrayOutputStream()
    val ps = new PrintStream(out)
    ps.println("```scala")
    ps.println(section.source.syntax)
    val crashes = for {
      statement <- section.section.statements
      binder <- statement.binders
      if binder.value.isInstanceOf[Crashed]
    } yield binder.value.asInstanceOf[Crashed]
    crashes.headOption match {
      case Some(CrashResult.Crashed(e, _)) =>
        VorkExceptions.trimStacktrace(e)
        e.printStackTrace(new PrintStream(out))
      case None =>
        val mpos = section.source.pos.toUnslicedPosition
        reporter.error(mpos, "Expected runtime exception but program completed successfully")
    }
    ps.println("```")
    out.toString()
  }

  def renderEvaluatedSection(
      doc: EvaluatedDocument,
      section: EvaluatedSection,
      reporter: Reporter
  ): String = {
    val sb = new StringBuilder
    var first = true
    section.section.statements.zip(section.source.stats).foreach {
      case (statement, tree) =>
        if (first) {
          first = false
        } else {
          sb.append("\n")
        }
        sb.append("@ ")
          .append(tree.syntax)
        if (statement.out.nonEmpty) {
          sb.append("\n").append(statement.out)
        }
        if (sb.charAt(sb.length() - 1) != '\n') {
          sb.append("\n")
        }

        statement.binders.foreach { binder =>
          section.mod match {
            case Modifier.Fail =>
              binder.value match {
                case CompileResult.TypecheckedOK(_, tpe, pos) =>
                  val mpos = Position
                    .Range(
                      section.input,
                      pos.startLine,
                      pos.startColumn,
                      pos.endLine,
                      pos.endColumn
                    )
                    .toUnslicedPosition
                  reporter.error(
                    mpos,
                    s"Expected compile error but statement type-checked successfully"
                  )
                  sb.append(s"// $tpe")
                case CompileResult.ParseError(msg, pos) =>
                  sb.append(pos.formatMessage(doc.edit, msg))
                case CompileResult.TypeError(msg, pos) =>
                  sb.append(pos.formatMessage(doc.edit, msg))
                case _ =>
                  val obtained = pprint.PPrinter.BlackWhite.apply(binder).toString()
                  throw new IllegalArgumentException(
                    s"Expected Macros.CompileResult." +
                      s"Obtained $obtained"
                  )
              }
            case Modifier.Default | Modifier.Passthrough =>
              sb.append(binder.name)
                .append(": ")
                .append(binder.tpe.render)
                .append(" = ")
                .append(pprint.PPrinter.BlackWhite.apply(binder.value))
                .append("\n")
            case Modifier.Crash =>
              throw new IllegalArgumentException(Modifier.Crash.toString)
            case c: Modifier.Str =>
              throw new IllegalArgumentException(c.toString)
          }
        }
    }
    if (sb.nonEmpty && sb.last == '\n') sb.setLength(sb.length - 1)
    sb.toString
  }

}
