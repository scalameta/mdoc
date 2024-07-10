package tests.js

import mdoc.internal.cli.Settings
import scala.meta.io.Classpath
import tests.markdown.StringSyntax._
import tests.cli.BaseCliSuite
import scala.meta.internal.io.PathIO
import tests.js.JsTests.suffix

class JsCliSuite extends BaseCliSuite {

  checkCli(
    "basic",
    """/index1.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |/index2.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |/index3.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |""".stripMargin,
    s"""|/index1.md
        |```scala
        |println("hello world!")
        |```
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |${suffix("index1")}
        |
        |/index2.md
        |```scala
        |println("hello world!")
        |```
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |${suffix("index2")}
        |""".stripMargin,
    input = "index1.md",
    extraArgs = Array("--in", "index2.md", "--out", out().toString()),
    includeOutputPath = { path => !path.toNIO.getFileName.toString.endsWith(".js") }
  )

  checkCli(
    "remap".only,
    """/index1.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |/index2.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |/index3.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |""".stripMargin,
    s"""|/index1.md
        |```scala
        |println("hello world!")
        |```
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |${suffix("index1")}
        |
        |/index2.md
        |```scala
        |println("hello world!")
        |```
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |${suffix("index2")}
        |""".stripMargin,
    input = "index1.md",
    extraArgs = Array(
      "--in",
      "index2.md",
      "--out",
      out().toString(),
      "--import-map-path",
      "/Users/simon/Code/mdoc-1/tests/unit-js/resources/importmap.json"
    ),
    includeOutputPath = { path => !path.toNIO.getFileName.toString.endsWith(".js") }
  )

}
