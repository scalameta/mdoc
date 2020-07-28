package mdoc.internal.markdown

import scala.collection.mutable
import scala.meta.Name
import scala.meta.io.AbsolutePath
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.Reporter
import dotty.tools.dotc.ast.untpd._
import dotty.tools.dotc.ast.Trees

class MagicImports(settings: Settings, reporter: Reporter, file: InputFile) {

  val scalacOptions = mutable.ListBuffer.empty[Name.Indeterminate]
  val dependencies = mutable.ListBuffer.empty[Name.Indeterminate]
  val repositories = mutable.ListBuffer.empty[Name.Indeterminate]
  val files = mutable.Map.empty[AbsolutePath, FileImport]

  object NonPrintable {
    def unapply(importer: Import): Boolean = importer match {
      case Import(
          Ident(qualifier),
          List(ImportSelector(Ident(name), _, _))
          ) if Instrumenter.magicImports(qualifier.toString) =>
        qualifier.toString match {
          case "$ivy" | "$dep" =>
            dependencies += Name.Indeterminate(name.toString)
            true
          case "$repo" =>
            repositories += Name.Indeterminate(name.toString)
            true
          case "$scalac" =>
            scalacOptions += Name.Indeterminate(name.toString)
            true
          case _ =>
            false
        }
      case _ => false
    }
  }
}
