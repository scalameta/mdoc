package mdoc.internal.markdown

import com.virtuslab.using_directives.UsingDirectivesProcessor
import com.virtuslab.using_directives.custom.utils.ast._
import com.virtuslab.{using_directives => using}
import mdoc.Reporter
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import mdoc.internal.pos.PositionSyntax._

import scala.collection.mutable
import scala.meta.Import
import scala.meta.Importee
import scala.meta.Importer
import scala.meta.Name
import scala.meta.Source
import scala.meta.Term
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.io.AbsolutePath
import scala.meta.parsers.Parsed.Success
import java.nio.file.Paths
import scala.util.control.NonFatal

case class MagicImport(value: String, pos: Position)

class MagicImports(settings: Settings, reporter: Reporter, file: InputFile) {

  import MagicImports._

  val scalacOptions = mutable.ListBuffer.empty[MagicImport]
  val dependencies = mutable.ListBuffer.empty[MagicImport]
  val repositories = mutable.ListBuffer.empty[MagicImport]
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
    def unapply(importer: Importer): Boolean =
      importer match {
        case Importer(
              Term.Name(qualifier),
              List(Importee.Name(name: Name.Indeterminate))
            ) if Instrumenter.magicImports(qualifier) =>
          qualifier match {
            case "$ivy" | "$dep" =>
              dependencies += MagicImport(name.value, name.pos)
              true
            case "$repo" =>
              repositories += MagicImport(name.value, name.pos)
              true
            case "$scalac" =>
              scalacOptions += MagicImport(name.value, name.pos)
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
    (input, MdocDialect.scala).parse[Source] match {
      case e: scala.meta.parsers.Parsed.Error =>
        reporter.error(e.pos, e.message)
      case scala.meta.parsers.Parsed.Success(s) =>
        s.stats.foreach {
          case i: Import =>
            i.importers.foreach {
              case importer @ FilePrintable(deps) =>
                deps.foreach { dep =>
                  dep.packageName match {
                    case Some(packageName) if packageName != importer.ref.syntax =>
                      renames += Rename(importer.ref.pos, packageName)
                    case _ =>
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

  def visitUsingFile(input: Input, visited: Set[AbsolutePath] = Set.empty): Unit = {

    val relativePath =
      try {
        input match {
          case Input.VirtualFile(path, _) =>
            file.inputFile.parent.resolve(path)
          case Input.Slice(input: Input.VirtualFile, _, _) =>
            file.inputFile.parent.resolve(input.path)
          case _ => file.inputFile
        }
      } catch {
        case NonFatal(_) => file.inputFile
      }

    val path = AbsolutePath(relativePath.toNIO.normalize())
    if (!visited.contains(path)) {
      val fileDependencies = mutable.ListBuffer.empty[FileImport]

      findUsingDirectivesWith(input) { (ud: UsingDef) =>
        ud.getKey() match {
          case "file" | "files" =>
            val imports = toStringValue(ud.getValue()).flatMap { usingImport =>
              FileImport.fromUsing(
                path,
                MagicImport(usingImport, toMetaPosition(input, ud.getPosition())),
                reporter
              )
            }
            fileDependencies ++= imports
          case _ =>
        }
      }
      val toVisit = fileDependencies.toList
      toVisit.foreach { fileImport =>
        files(fileImport.path) = fileImport
        visitUsingFile(
          Input.VirtualFile(fileImport.path.toString(), fileImport.source),
          visited + path
        )
      }
    }
  }

  def findUsingDirectives(input: Input) = findUsingDirectivesWith(input) { (ud: UsingDef) =>
    ud.getKey() match {
      case "dep" | "dependency" | "lib" | "library" =>
        toStringValue(ud.getValue()).foreach {
          dependencies += MagicImport(_, toMetaPosition(input, ud.getPosition()))
        }

      case "option" | "options" | "scalac" =>
        toStringValue(ud.getValue()).foreach {
          scalacOptions += MagicImport(_, toMetaPosition(input, ud.getPosition()))
        }

      case "file" | "files" =>
      case "repo" | "repository" =>
        toStringValue(ud.getValue()).foreach {
          repositories += MagicImport(_, toMetaPosition(input, ud.getPosition()))
        }
      case _: String =>
        reporter.warning(
          toMetaPosition(input, ud.getPosition()),
          s"Unknown directive: ${ud.getKey()}"
        )

    }

  }

  private def findUsingDirectivesWith(input: Input)(
      convert: UsingDef => Unit
  ) = if (!settings.disableUsingDirectives) {

    val usingReporter = new UsingReporter(input, reporter)
    val processor = new UsingDirectivesProcessor(usingReporter)
    val allDirectives = processor.extract(input.chars).asScala

    allDirectives.foreach { directives =>
      directives.getAst match {
        case uds: UsingDefs => uds.getUsingDefs.asScala.toSeq.foreach(convert)
        case ud: UsingDef => convert(ud)
        case _ =>

      }
    }
  }

  private def toStringValue(value: UsingValue): List[String] = value match {
    case sl: StringLiteral => List(sl.getValue())
    case values: UsingValues => values.getValues().asScala.toList.flatMap(toStringValue)
    case _ => Nil
  }

}

object MagicImports {
  def toMetaPosition(input: Input, position: using.custom.utils.Position) = {
    scala.meta.inputs.Position.Range(input, position.getOffset(), position.getOffset())
  }
}
