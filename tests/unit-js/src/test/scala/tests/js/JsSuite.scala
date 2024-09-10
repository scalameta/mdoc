package tests.js

import mdoc.internal.cli.Settings
import scala.meta.io.Classpath
import tests.markdown.StringSyntax._
import tests.markdown.BaseMarkdownSuite
import tests.js.JsTests.suffix
import tests.markdown.Compat
import scala.meta.io.AbsolutePath

class JsSuite extends BaseMarkdownSuite {
  // NOTE(olafur) Optimization. Cache settings to reuse the Scala.js compiler instance.
  // By default, we create new modifiers for each unit test, which is usually fast.
  override def baseSettings(resourcePropertyFileName: String): Settings = super
    .baseSettings()
    .copy(
      site = super.baseSettings().site ++ Map(
        "js-opt" -> "fast"
      )
    )

  check(
    "basic",
    """
      |```scala mdoc:js
      |println("hello world!")
      |```
      |""".stripMargin,
    """|```scala
       |println("hello world!")
       |```
       |<div id="mdoc-html-run0" data-mdoc-js></div>
       |<script type="text/javascript" src="basic.md.js" defer></script>
       |<script type="text/javascript" src="mdoc.js" defer></script>
    """.stripMargin
  )

  check(
    "basic_es",
    """
      |```scala mdoc:js
      |println("hello world!")
      |```
      |""".stripMargin,
    """|```scala
       |println("hello world!")
       |```
       |<div id="mdoc-html-run0" data-mdoc-js data-mdoc-module-name="./basic_es.md.js" ></div>
       |<script type="module" src="basic_es.md.js"></script>
       |<script type="module" src="mdoc.js"></script>
    """.stripMargin,
    settings = {
      baseSettings().copy(
        site = baseSettings().site.updated("js-module-kind", "ESModule")
      )
    }
  )

  checkCompiles(
    "es_remap_settings",
    """
      |```scala mdoc:js:shared
      |
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
      |
      |```scala mdoc:js
      |println(blas)
      |```
      |""".stripMargin,
    settings = {
      baseSettings().copy(
        site = baseSettings().site.updated("js-module-kind", "ESModule"),
        importMapPath = Some(
          AbsolutePath(this.getClass.getClassLoader.getResource("importmap.json").getPath.toNIO)
        )
      )
    }
  )

  check(
    "extra_fences",
    """
      |```scala mdoc:js sc:nocompile
      |println("hello world!")
      |```
      |""".stripMargin,
    """|```scala sc:nocompile
       |println("hello world!")
       |```
       |<div id="mdoc-html-run0" data-mdoc-js></div>
       |<script type="text/javascript" src="extra_fences.md.js" defer></script>
       |<script type="text/javascript" src="mdoc.js" defer></script>
    """.stripMargin
  )

  checkError(
    "error",
    """
      |```scala mdoc:js
      |val x: Int = ""
      |```
    """.stripMargin,
    """
      |error: error.md:3:14: type mismatch;
      | found   : String("")
      | required: Int
      |val x: Int = ""
      |             ^^
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error: error.md:3:14:
          |Found:    ("" : String)
          |Required: Int
          |val x: Int = ""
          |             ^^
      """.stripMargin
    )
  )

  check(
    "multi",
    """
      |```scala mdoc:js
      |println("hello 1!")
      |```
      |
      |```scala mdoc:js
      |println("hello 2!")
      |```
      |""".stripMargin,
    """|```scala
       |println("hello 1!")
       |```
       |<div id="mdoc-html-run0" data-mdoc-js></div>
       |
       |```scala
       |println("hello 2!")
       |```
       |<div id="mdoc-html-run1" data-mdoc-js></div>
       |<script type="text/javascript" src="multi.md.js" defer></script>
       |<script type="text/javascript" src="mdoc.js" defer></script>
       |""".stripMargin
  )

  checkError(
    "edit",
    """
      |```scala mdoc:js
      |val x: Int = ""
      |```
      |
      |```scala mdoc:js
      |val y: String = 42
      |```
    """.stripMargin,
    """|error: edit.md:3:14: type mismatch;
       | found   : String("")
       | required: Int
       |val x: Int = ""
       |             ^^
       |error: edit.md:7:17: type mismatch;
       | found   : Int(42)
       | required: String
       |val y: String = 42
       |                ^^
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error: edit.md:3:14:
          |Found:    ("" : String)
          |Required: Int
          |val x: Int = ""
          |             ^^
          |error: edit.md:7:17:
          |Found:    (42 : Int)
          |Required: String
          |val y: String = 42
          |                ^^
      """.stripMargin
    )
  )

  checkError(
    "isolated",
    """
      |```scala mdoc:js
      |val x = 1
      |```
      |
      |```scala mdoc:js
      |println(x)
      |```
    """.stripMargin,
    """|error: isolated.md:7:9: not found: value x
       |println(x)
       |        ^
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error: isolated.md:7:9:
          |Not found: x
          |println(x)
          |        ^
      """.stripMargin
    )
  )

  checkCompiles(
    "mountNode",
    """
      |```scala mdoc:js
      |node.innerHTML = "<h3>Hello world!</h3>"
      |```
    """.stripMargin
  )

  check(
    "shared",
    """
      |```scala mdoc:js:shared
      |val x = 1
      |```
      |
      |```scala mdoc:js
      |println(x)
      |```
      |""".stripMargin,
    """|```scala
       |val x = 1
       |```
       |
       |```scala
       |println(x)
       |```
       |<div id="mdoc-html-run1" data-mdoc-js></div>
       |<script type="text/javascript" src="shared.md.js" defer></script>
       |<script type="text/javascript" src="mdoc.js" defer></script>
    """.stripMargin
  )

  check(
    "compile-only",
    """
      |```scala mdoc:compile-only
      |println(42)
      |```
    """.stripMargin,
    """|```scala
       |println(42)
       |```
    """.stripMargin
  )

  checkError(
    "compile-only-error",
    """
      |```scala mdoc:compile-only
      |val x: String = 42
      |```
    """.stripMargin,
    """|error: compile-only-error.md:3:17: type mismatch;
       | found   : Int(42)
       | required: String
       |val x: String = 42
       |                ^^
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error: compile-only-error.md:3:17:
          |Found:    (42 : Int)
          |Required: String
          |val x: String = 42
          |                ^^
      """.stripMargin
    )
  )

  // It's easy to mess up stripMargin multiline strings when generating code with strings.
  check(
    "stripMargin",
    """
      |```scala mdoc:js
      |val x = '''
      |  |a
      |  | b
      |  |  c
      | '''.stripMargin
      |```
      |""".stripMargin.triplequoted,
    s"""|```scala
        |val x = '''
        |  |a
        |  | b
        |  |  c
        | '''.stripMargin
        |```
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |${suffix("stripMargin")}
        |""".stripMargin.triplequoted
  )

  check(
    "invisible",
    """
      |```scala mdoc:js:invisible
      |println("Hello!")
      |```
      |""".stripMargin,
    s"""|
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |${suffix("invisible")}
        |""".stripMargin
  )

  checkCompiles(
    "deps",
    """
      |```scala mdoc:js
      |println(jsdocs.ExampleJS.greeting)
      |```
    """.stripMargin
  )

  checkCompiles(
    "onclick",
    """
      |```scala mdoc:js
      |import org.scalajs.dom.MouseEvent
      |node.onclick = {(_: MouseEvent) => println(42)}
      |```
    """.stripMargin
  )

  checkError(
    "no-dom",
    """
      |```scala mdoc:js
      |println(jsdocs.ExampleJS.greeting)
      |```
    """.stripMargin,
    """|error: no-dom.md:4 (mdoc generated code) object dom is not a member of package org.scalajs
       |def run0(node: _root_.org.scalajs.dom.html.Element): Unit = {
       |                                  ^
    """.stripMargin,
    settings = {
      val noScalajsDom = Classpath(baseSettings().site("js-classpath")).entries
        .filterNot(_.toNIO.getFileName.toString.contains("scalajs-dom"))
      baseSettings().copy(
        site = baseSettings().site.updated("js-classpath", Classpath(noScalajsDom).syntax)
      )
    },
    compat = Map(
      Compat.Scala3 ->
        """
          |error:
          |no-dom.md:3 (mdoc generated code)
          | value dom is not a member of org.scalajs
          |def run0(node: _root_.org.scalajs.dom.html.Element): Unit = {
      """.stripMargin
    )
  )

  checkError(
    "mods-error",
    """|
       |```scala mdoc:js:shared:not
       |println(1)
       |```
       |""".stripMargin,
    """|error: mods-error.md:2:25: invalid modifier 'not'
       |```scala mdoc:js:shared:not
       |                        ^^^
    """.stripMargin
  )

  check(
    "commonjs",
    """
      |```scala mdoc:js
      |println("Hello!")
      |```
      |""".stripMargin,
    """|```scala
       |println("Hello!")
       |```
       |<div id="mdoc-html-run0" data-mdoc-js></div>
       |<script type="text/javascript" src="mdoc-library.js" defer></script>
       |<script type="text/javascript" src="mdoc-loader.js" defer></script>
       |<script type="text/javascript" src="commonjs.md.js" defer></script>
       |<script type="text/javascript" src="mdoc.js" defer></script>
       |""".stripMargin,
    settings = {
      val libraries = List(
        createTempFile("mdoc-loader.js"),
        createTempFile("mdoc-ignoreme.md"),
        createTempFile("mdoc-library.js"),
        createTempFile("mdoc-library.js.map")
      )
      baseSettings().copy(
        site = baseSettings().site
          .updated("js-module-kind", "CommonJSModule")
          .updated("js-libraries", Classpath(libraries).syntax)
      )
    }
  )

  def unpkgReact =
    """<script crossorigin src="https://unpkg.com/react@16.5.1/umd/react.production.min.js"></script>"""
  check(
    "html-header",
    """
      |```scala mdoc:js:invisible
      |println("Hello!")
      |```
      |""".stripMargin,
    s"""|
        |<div id="mdoc-html-run0" data-mdoc-js></div>
        |$unpkgReact
        |<script type="text/javascript" src="html-header.md.js" defer></script>
        |<script type="text/javascript" src="mdoc.js" defer></script>
        |""".stripMargin,
    settings = {
      baseSettings().copy(
        site = baseSettings().site.updated("js-html-header", unpkgReact)
      )
    }
  )
}
