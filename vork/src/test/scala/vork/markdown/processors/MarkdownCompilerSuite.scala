package vork.markdown.processors

import org.scalatest.FunSuite
import scala.meta._
import scala.meta.testkit.DiffAssertions
import scalafix._

class MarkdownCompilerSuite extends FunSuite with DiffAssertions {
  def instrument(md: String): String = {
    val source = dialects.Sbt1(md).parse[Source].get
    val stats = source.stats
    val ctx = RuleCtx(source)
    val rule = Rule.syntactic("Vork") { ctx =>
      val patches = stats.map {
        case stat @ Defn.Val(_, pats, _, _) =>
          val names = pats.flatMap { pat =>
            pat.collect { case m: Member => m.name }
          }
          val binders = names.map(name => s"binder($name)").mkString(";", ";", ";")
          ctx.addRight(stat, binders)
      }
      val patch = patches.asPatch
      patch
    }
    rule.apply(ctx)
  }

  test("instrument") {
    val markdown =
      """
        |val x = 1.to(10)
        |val List(y, z) = x.map(_ + 1).toList.take(2)
      """.stripMargin
    val out = instrument(markdown)
    assertNoDiff(
      out,
      """"""
    )
  }

  ignore("compiler") {
    val compiler = MarkdownCompiler.default()
    val doc = MarkdownCompiler.document(compiler)
    pprint.log(doc)
  }

}
