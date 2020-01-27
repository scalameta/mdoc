package mdoc.modifiers

import mdoc.Reporter
import mdoc.StringModifier
import scala.meta.inputs.Input
import java.util.UUID

/** Transforms Scala code blocks into Scastie snippets
  *
  * ==Usage==
  *
  * ===Inline code===
  * {{{
  * ```scala mdoc:scastie
  * println("Hello, Scastie!")
  * ```
  * }}}
  * ====Note====
  * This will be slower to run than embedding a snippet, since it's not cached
  *
  * ===Anonymous snippet===
  * {{{
  * ```scala mdoc:scastie:seL6YZuTSu65HiC1rLNwmQ
  *
  * ```
  * }}}
  * ====Note====
  * The empty line in the block is relevant (md parser chokes otherwise)
  *
  * ===User's snippet===
  * {{{
  * ```scala mdoc:scastie:MasseGuillaume/33D4P3ysQCq2em2MRiv5sQ
  *
  * ```
  * }}}
  * ====Note====
  * The empty line in the block is relevant (md parser chokes otherwise)
  */
class ScastieModifier(theme: String = "light", debugClassSuffix: Option[String] = None)
    extends StringModifier {
  override val name: String = "scastie"
  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    val snippetId = if (info.length > 0) Some(info) else None

    snippetId match {
      case Some(snippetId) => scastieSnippet(snippetId)
      case None => inlineScastieSnippet(code.text)
    }

  }

  def this() = this("light", None)

  def scastieSnippet(snippetId: String): String = {
    s"<script src='https://scastie.scala-lang.org/${snippetId}.js?theme=$theme'></script>"
  }

  def inlineScastieSnippet(code: String): String = {
    val classSuffix = debugClassSuffix.getOrElse(UUID.randomUUID)
    val targetClass = s"scastie-snippet-$classSuffix"
    val embedCode = List(
      s" scastie.Embedded('.$targetClass', {",
      s"   code: `${code}`,",
      s"   theme: '$theme',",
      "    isWorksheetMode: true,",
      "    targetType: 'jvm',",
      "    scalaVersion: '2.12.6'",
      "  })"
    ).mkString("\n")

    List(
      "<script src=\"https://scastie.scala-lang.org/embedded.js\"></script>",
      s"<pre class='$targetClass'></pre>",
      "<script>window.addEventListener('load', function() {",
      embedCode,
      "})</script>"
    ).mkString("\n")
  }

}
