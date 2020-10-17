package mdoc.docs

import mdoc.Reporter
import mdoc.StringModifier
import mdoc.MdocPlugin.autoImport._
import scala.meta.inputs.Input

class SbtModifier extends StringModifier {
  override val name: String = "sbt"

  override def process(info: String, code: Input, reporter: Reporter): String = {
    val keys = List(
      mdoc,
      mdocIn,
      mdocOut,
      mdocVariables,
      mdocExtraArguments,
      mdocJS,
      mdocJSLibraries,
      mdocAutoDependency
    )
    val rows = keys.map { s =>
      val tpe = s.key.manifest
        .toString()
        .replaceAllLiterally("java.lang.String", "String")
        .replaceAllLiterally("scala.collection.immutable.", "")
        .replaceAllLiterally("scala.collection.", "")
        .replaceAllLiterally("sbt.internal.util.", "")
      <tr>
        <td><code>{s.key.toString}</code></td>
        <td><code>{tpe}</code></td>
        <td>{s.key.description.getOrElse("")}</td>
      </tr>
    }
    <table>
      <tr>
        <th>Task</th>
        <th>Type</th>
        <th>Description</th>
      </tr>
      {rows}
    </table>
  }.toString() + "\n\n"
}
