package tests.js

import mdoc.internal.cli.MdocProperties
import mdoc.internal.cli.Settings
import scala.meta.io.Classpath
import tests.markdown.StringSyntax._
import tests.cli.BaseCliSuite
import scala.meta.internal.io.PathIO
import tests.js.JsTests.suffix
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.io.Source

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
    "basic_es",
    """/docs/index1.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |/docs/index2.md
      |```scala mdoc:js
      |println("hello world!")
      |```
      |""".stripMargin,
    s"""|/docs/index1.md
        |```scala
        |println("hello world!")
        |```
        |<div id="mdoc-html-run0" data-mdoc-js data-mdoc-module-name="./docs/index1.md.js" ></div>
        |<script type="module" src="index1.md.js"></script>
        |<script type="module" src="mdoc.js"></script>
        |
        |
        |/docs/index2.md
        |```scala
        |println("hello world!")
        |```
        |<div id="mdoc-html-run0" data-mdoc-js data-mdoc-module-name="./docs/index2.md.js" ></div>
        |<script type="module" src="index2.md.js"></script>
        |<script type="module" src="mdoc.js"></script>""".stripMargin,
    extraArgs = Array("--property-file-name", "es.properties"),
    includeOutputPath = { path => !path.toNIO.getFileName.toString.endsWith(".js") }
  )

  /** See MdocPlugin for where the .properties file is generated.
    */

  // see the importmap.json in resources
  test("import remap") {
    val myStdout = new ByteArrayOutputStream()
    tests.cli.StringFS.fromString(
      """|/docs/facade.md
         |  make a facade
         |```scala mdoc:js:shared
         |import scala.scalajs.js
         |import scala.scalajs.js.annotation.JSImport
         |
         |@js.native
         |@JSImport("@stdlib/blas/base", JSImport.Namespace)
         |object blas extends BlasArrayOps
         |
         |@js.native
         |trait BlasArrayOps extends js.Object{}
         |```
         |Now fake do something with the facade
         |```scala mdoc:js
         |println(blas)
         |```""".stripMargin,
      in()
    )
    val args = Array[String](
      "--in",
      in().toString,
      "--out",
      out().toString,
      "--cwd",
      in().syntax,
      "--property-file-name",
      "es.properties",
      "--import-map-path",
      Paths.get(this.getClass.getClassLoader.getResource("importmap.json").toURI).toString()
    )
    val code = mdoc.Main.process(args, new PrintStream(myStdout), in().toNIO)
    val generatedJs =
      AbsolutePath(out().toNIO.resolve("docs").resolve("facade.md.js"))
    val content = Source.fromFile(generatedJs.toNIO.toUri()).getLines().toList
    println(content(1))

    /** SP 16.07.2024 assert(content(contains("https://cdn.jsdelivr.net/npm/@stdlib/blas")) The line
      * above should also be an effective test. It does not illustrate what the remap tries to
      * achieve, so the test below is preferred.
      *
      * However, given that the names in the test below are "generated", by scalaJS there is a risk
      * the test turns flaky / fails at some point in the futurem, should scalaJS change it's with
      * regard to variable name generation. It this test turns "flaky" the line above could be used
      * in that case to reduce maintenance burden.
      */

    assertEquals(
      content(1),
      """import * as $i_https$003a$002f$002fcdn$002ejsdelivr$002enet$002fnpm$002f$0040stdlib$002fblas$00400$002e2$002e1$002f$002besm$002fbase from "https://cdn.jsdelivr.net/npm/@stdlib/blas@0.2.1/+esm/base";"""
    )
    assertEquals(code, 0, clues(myStdout))

  }

}
