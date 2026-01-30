package tests.interface

import mdoc.interfaces.DiagnosticSeverity
import mdoc.interfaces.Mdoc
import mdoc.internal.CompatClassloader
import mdoc.internal.pos.PositionSyntax._
import munit.Location
import munit.TestOptions
import tests.BaseSuite
import tests.markdown.Compat

import java.lang.StringBuilder
import java.nio.file.Paths
import java.{util => ju}
import scala.meta.inputs.Input
import scala.meta.inputs.Position

class EvaluatedMarkdownDocumentSuite extends BaseSuite {

  var mdoc = ju.ServiceLoader
    .load(classOf[Mdoc], this.getClass().getClassLoader())
    .iterator()
    .next()
    .withScreenHeight(5)
    .withClasspath(
      CompatClassloader
        .getURLs(this.getClass().getClassLoader())
        .collect {
          case url
              if url.toString.contains("scala3-library") ||
                url.toString
                  .contains("scala-library") =>
            Paths.get(url.toURI())
        }
        .asJava
    )

  override def afterAll(): Unit = {
    mdoc.shutdown()
  }

  test("basic markdown evaluation") {
    val document =
      """
        |# Hello world!
        | 
        |The latest library version is `@HELLO@.@WORLD@`
        | 
        |```scala mdoc
        |println(math.abs(-25))
        |```
      """.stripMargin

    val expectedContent =
      """
        |# Hello world!
        | 
        |The latest library version is `1.2`
        | 
        |```scala
        |println(math.abs(-25))
        |// 25
        |```
      """.stripMargin

    val variables = new java.util.HashMap[String, String]()
    variables.put("HELLO", "1")
    variables.put("WORLD", "2")

    val evaluated = mdoc.evaluateMarkdownDocument("README.md", document, variables)

    assertEquals(evaluated.content(), expectedContent)
  }
}
