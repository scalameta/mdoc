package tests.js

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import mdoc.internal.io.ConsoleReporter
import mdoc.modifiers.JsMods
import scala.meta.inputs.Input
import tests.markdown.BaseMarkdownSuite

class JsModsSuite extends BaseMarkdownSuite {
  def parsed(info: String, fn: (String, Option[JsMods]) => Unit): Unit = {
    test(if (info.isEmpty) "<empty>" else info) {
      val input = Input.String(info)
      val out = new ByteArrayOutputStream()
      val reporter = new ConsoleReporter(new PrintStream(out))
      val mods = JsMods.parse(input, reporter)
      val stdout = fansi.Str(out.toString(StandardCharsets.UTF_8.name))
      fn(stdout.plainText, mods)
    }
  }
  def checkError(str: String, expected: String): Unit = {
    parsed(str, (err, res) => {
      assertNoDiff(err, expected)
      assert(res.isEmpty)
    })
  }
  def checkOK(str: String): Unit = {
    parsed(str, (err, res) => {
      assertNoDiff(err, "")
      assert(res.isDefined)
    })
  }
  checkOK("")
  checkOK("shared")
  checkOK("invisible")
  checkOK("shared:invisible")
  checkError(
    "shared:invisible:foo",
    """|error: <input>:1:18: invalid modifier 'foo'
       |shared:invisible:foo
       |                 ^^^
    """.stripMargin
  )
  checkError(
    "compile-only:invisible",
    """|error: <input>:1:1: compile-only cannot be used in combination with invisible
       |compile-only:invisible
       |^^^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )
}
