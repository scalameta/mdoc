package vork.markdown.processors

import org.scalatest.FunSuite
import scala.meta._
import scala.meta.testkit.DiffAssertions
import scalafix._

class MarkdownCompilerSuite extends FunSuite with DiffAssertions {
  def instrumentSections(sections: List[String]): String = {
    val stats = sections.map(instrument)
    val out = stats.foldRight("") {
      case (section, "") =>
        s"$section; section { () }"
      case (section, accum) =>
        s"$section; section { $accum }"
    }
    out
  }

  def instrument(md: String): String = {
    val source = dialects.Sbt1(md).parse[Source].get
    val stats = source.stats
    val ctx = RuleCtx(source)
    val rule = Rule.syntactic("Vork") { ctx =>
      val last = ctx.tokens.last
      val patches = stats.map {
        case stat @ Defn.Val(_, pats, _, _) =>
          val names = pats.flatMap { pat =>
            pat.collect { case m: Member => m.name }
          }
          val binders = names
            .map(name => s"binder($name)")
            .mkString(";", ";", "; statement {")
          ctx.addRight(stat, binders) +
            ctx.addRight(last, " }")
      }
      val patch = patches.asPatch
      patch
    }
    rule.apply(ctx)
  }

  val compiler = MarkdownCompiler.default()

  test("instrument") {
    val md1 =
      """
        |val x = 1.to(10)
        |val List(y, z) = x.map(_ + 1).toList.take(2)
      """.stripMargin
    val md2 =
      """
        |val y = "msg"
        |val z = y.length
      """.stripMargin
    val out = instrumentSections(md1 :: md2 :: Nil)
    val doc = MarkdownCompiler.document(compiler, out)
    pprint.log(doc)
    assertNoDiff(
      out,
      """"""
    )
  }

  ignore("compiler") {
    val doc = MarkdownCompiler.document(compiler, "")
    pprint.log(doc)
  }

}
