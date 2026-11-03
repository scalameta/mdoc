package tests.js

import mdoc.internal.cli.Context
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Markdown
import mdoc.internal.markdown.MarkdownBuilder
import tests.js.JsTests.suffix
import tests.markdown.BaseMarkdownSuite

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import scala.meta.inputs.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath

class JsScala3LinkerSuite extends BaseMarkdownSuite {

  /* The class loader of the worker takes each class from the first entry that has it, so the
   * Scala 3 build of the linker replaces the Scala 2.13 build that sbt-mdoc puts after it. */
  override def baseSettings(resourcePropertyFileName: String): Settings = {
    val settings = super.baseSettings(JsTests.propertyFileName)
    val sbtMdoc = Classpath(settings.site("js-linker-classpath")).entries
    val linker = BuildInfo.scalaJSLinker3Classpath.map(AbsolutePath(_)).toList ++ sbtMdoc
    settings.copy(site = settings.site.updated("js-linker-classpath", Classpath(linker).syntax))
  }

  private val compiler = MarkdownBuilder.fromClasspath("", Nil)

  // munit stops the suite on a LinkageError, so the error becomes the result
  private def process(name: String, original: String, settings: Settings): String =
    try {
      val stdout = new ByteArrayOutputStream()
      val reporter = new ConsoleReporter(new PrintStream(stdout))
      settings.onLoad(reporter)
      val context = Context.fromCompiler(settings, reporter, compiler)
      val input = Input.VirtualFile(name, original)
      val file = InputFile.fromRelativeFilename(name, settings)
      val obtained = Markdown.toMarkdown(input, context, file, reporter, settings)
      assert(!reporter.hasErrors, stdout)
      obtained
    } catch { case e: LinkageError => e.toString }

  private val noInit =
    "java.lang.NoSuchMethodError: 'void org.scalajs.logging.Logger.$init$(org.scalajs.logging.Logger)'"

  test("mdoc:js") {
    val original =
      """|```scala mdoc:js
         |println("hello world!")
         |```
         |""".stripMargin
    assertNoDiff(
      process("basic.md", original, baseSettings()),
      s"""|```scala
          |println("hello world!")
          |```
          |<div id="mdoc-html-run0" data-mdoc-js></div>
          |${suffix("basic")}
          |""".stripMargin
    )
  }

  test("mdoc:js with an import map") {
    val original =
      """|```scala mdoc:js:shared
         |import scala.scalajs.js
         |import scala.scalajs.js.annotation.JSImport
         |
         |@js.native
         |@JSImport("@stdlib/blas/base", JSImport.Namespace)
         |object blas extends js.Object
         |```
         |```scala mdoc:js
         |println(blas)
         |```
         |""".stripMargin
    val base = baseSettings()
    val settings = base.copy(
      site = base.site.updated("js-module-kind", "ESModule"),
      importMapPath = Some(AbsolutePath(JsTests.importMap))
    )
    assertNoDiff(
      process("facade.md", original, settings),
      """|```scala
         |import scala.scalajs.js
         |import scala.scalajs.js.annotation.JSImport
         |
         |@js.native
         |@JSImport("@stdlib/blas/base", JSImport.Namespace)
         |object blas extends js.Object
         |```
         |```scala
         |println(blas)
         |```
         |<div id="mdoc-html-run1" data-mdoc-js data-mdoc-module-name="./facade.md.js" ></div>
         |<script type="module" src="facade.md.js"></script>
         |<script type="module" src="mdoc.js"></script>
         |""".stripMargin
    )
    // the import map in the test resources maps @stdlib/blas to this URL
    val js = FileIO.slurp(settings.out.head.resolve("facade.md.js"), StandardCharsets.UTF_8)
    assert(js.contains("from \"https://cdn.jsdelivr.net/npm/@stdlib/blas@0.2.1/+esm/base\""), js)
  }

}
