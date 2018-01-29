package vork.markdown.processors

import org.scalatest.FunSuite

class MarkdownCompilerTest extends FunSuite {
  test("compiler") {
    val compiler = MarkdownCompiler.default()
    val doc = MarkdownCompiler.document(compiler)
    pprint.log(doc)
  }

}
