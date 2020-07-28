package mdoc.internal.markdown

import scala.collection.mutable
import scala.meta.Name
import scala.meta.io.AbsolutePath
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import scala.meta.Importer
import mdoc.Reporter
import scala.meta.Importee
import scala.meta.Term
import scala.meta.inputs.Input
import scala.meta.parsers.Parsed.Success
import scala.meta.Source
import scala.meta.Import
import mdoc.internal.pos.PositionSyntax._
import scala.meta.inputs.Position

class MagicImports(settings: Settings, reporter: Reporter, file: InputFile) {

  val scalacOptions = mutable.ListBuffer.empty[Name.Indeterminate]
  val dependencies = mutable.ListBuffer.empty[Name.Indeterminate]
  val repositories = mutable.ListBuffer.empty[Name.Indeterminate]
  val files = mutable.Map.empty[AbsolutePath, FileImport]

  class Printable(inputFile: InputFile, parents: List[FileImport]) {
    private val File = new FileImport.Matcher(inputFile, reporter)
    def unapply(importer: Importer): Option[List[FileImport]] = {
      importer match {
        case File(fileImports) =>
          Some(fileImports.map(i => visitFile(i, parents)))
        case _ =>
          None
      }
    }
  }
  object Printable extends Printable(file, Nil)

  object NonPrintable {
    def unapply(importer: Importer): Boolean = importer match {
      case Importer(
          Term.Name(qualifier),
          List(Importee.Name(name: Name.Indeterminate))
          ) if Instrumenter.magicImports(qualifier) =>
        qualifier match {
          case "$ivy" | "$dep" =>
            dependencies += name
            true
          case "$repo" =>
            repositories += name
            true
          case "$scalac" =>
            scalacOptions += name
            true
          case _ =>
            false
        }
      case _ => false
    }
  }

  private def visitFile(fileImport: FileImport, parents: List[FileImport]): FileImport = {
    if (parents.exists(_.path == fileImport.path)) {
      val all = (parents.reverse :+ fileImport).map(_.importName.pos.toUnslicedPosition)
      val cycle = all
        .map(pos => s"${pos.input.filename}:${pos.startLine}")
        .mkString(
          s"\n -- root       --> ",
          s"\n -- depends on --> ",
          s"\n -- cycle      --> ${fileImport.path}"
        )
      reporter.error(
        all.head,
        s"illegal cyclic dependency. " +
          s"To fix this problem, refactor the code so that no transitive $$file imports end " +
          s"up depending on the original file.$cycle"
      )
      fileImport
    } else {
      files.getOrElseUpdate(fileImport.path, visitFileUncached(fileImport, parents))
    }
  }
  private def visitFileUncached(fileImport: FileImport, parents: List[FileImport]): FileImport = {
    val input = Input.VirtualFile(fileImport.path.toString(), fileImport.source)
    val FilePrintable = new Printable(
      InputFile.fromRelativeFilename(
        fileImport.path.toRelative(this.file.inputFile.parent).toString(),
        settings
      ),
      fileImport :: parents
    )
    val fileDependencies = mutable.ListBuffer.empty[FileImport]
    val renames = mutable.ListBuffer.empty[Rename]
    MdocDialect.scala(input).parse[Source] match {
      case e: scala.meta.parsers.Parsed.Error =>
        reporter.error(e.pos, e.message)
      case Success(source) =>
        source.stats.foreach {
          case i: Import =>
            i.importers.foreach {
              case importer @ FilePrintable(deps) =>
                deps.foreach { dep =>
                  if (importer.ref.syntax != dep.packageName) {
                    renames += Rename(importer.ref.pos, dep.packageName)
                  }
                  fileDependencies += dep
                }
              case NonPrintable() =>
              case _ =>
            }
          case _ =>
        }
    }
    fileImport.copy(
      dependencies = fileDependencies.toList,
      renames = renames.toList
    )
  }
}
