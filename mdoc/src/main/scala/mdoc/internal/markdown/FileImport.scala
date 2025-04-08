package mdoc.internal.markdown

import scala.meta.io.AbsolutePath
import java.nio.file.Path
import scala.meta.Name
import scala.meta.inputs.Input
import mdoc.internal.pos.PositionSyntax._
import scala.meta.Importee
import scala.meta.Term
import mdoc.Reporter
import scala.meta.Importer
import mdoc.internal.cli.InputFile
import scala.collection.mutable
import scala.meta.inputs.Position
import mdoc.internal.pos.TokenEditDistance
import scala.meta.Import
import mdoc.interfaces.ImportedScriptFile
import scala.meta._

final case class FileImport(
    path: AbsolutePath,
    importName: MagicImport,
    objectName: String,
    packageName: Option[String],
    source: String,
    dependencies: List[FileImport],
    renames: List[Rename]
) {
  val fullyQualifiedName = s"$packageName.$objectName"
  val prefix: String =
    packageName match {
      case None => s"object $objectName {"
      case Some(name) =>
        s"package $name; object $objectName {"

    }
  val toInput: Input = {
    val out = new java.lang.StringBuilder().append(prefix)
    var i = 0
    renames.sortBy(_.from.start).foreach { rename =>
      out
        .append(source, i, rename.from.start)
        .append(rename.to)
      i = rename.from.end
    }
    out
      .append(source, i, source.length())
      .append("\n}\n")
    Input.VirtualFile(path.syntax, out.toString())
  }
  val edit = TokenEditDistance(Input.VirtualFile(path.syntax, source), toInput)
  def toInterface: ImportedScriptFile = {
    mdoc.internal.worksheets.ImportedScriptFile(
      path.toNIO,
      packageName.getOrElse(""),
      objectName,
      toInput.text,
      source,
      dependencies.map(_.toInterface).asJava
    )
  }
}
object FileImport {
  class Matcher(
      file: InputFile,
      reporter: Reporter
  ) {
    def unapply(importer: Importer): Option[List[FileImport]] =
      importer match {
        case importer @ Importer(qual, importees) if isFileQualifier(qual) =>
          val parsed = FileImport.fromImportees(file.inputFile, qual, importees, reporter)
          if (parsed.forall(_.isDefined)) Some(parsed.map(_.get))
          else None
        case _ =>
          None
      }
    private def isFileQualifier(qual: Term): Boolean =
      qual match {
        case Term.Name("$file") => true
        case Term.Select(next, _) => isFileQualifier(next)
        case _ => false
      }
  }

  private def fromImportees(
      base: AbsolutePath,
      qual: Term,
      importees: List[Importee],
      reporter: Reporter
  ): List[Option[FileImport]] = {
    importees.collect {
      case Importee.Name(name: Name.Indeterminate) =>
        fromImport(base, qual, name, reporter)
      case Importee.Rename(name: Name.Indeterminate, _) =>
        fromImport(base, qual, name, reporter)
      case i @ Importee.Wildcard() =>
        reporter.error(
          i.pos,
          "wildcards are not supported for $file imports. " +
            "To fix this problem, explicitly import files using the `import $file.FILENAME` syntax."
        )
        None
      case i @ Importee.Unimport(_) =>
        reporter.error(
          i.pos,
          "unimports are not supported for $file imports. " +
            "To fix this problem, remove the unimported symbol."
        )
        None
    }
  }

  def fromUsing(
      base: AbsolutePath,
      fileImport: MagicImport,
      reporter: Reporter
  ) = {
    val pathToFile = base.parent.resolve(fileImport.value)
    val scriptPath =
      if (fileImport.value.endsWith(".sc")) {
        pathToFile
      } else {
        pathToFile.resolveSibling(_ + ".sc")
      }
    val absolute = AbsolutePath(scriptPath.toNIO.normalize())
    if (absolute.isFile) {
      val text = absolute.readText
      Some(
        FileImport(
          path = absolute,
          importName = fileImport,
          objectName = absolute.filename.stripSuffix(".sc"),
          packageName = None,
          source = text,
          dependencies = Nil,
          renames = Nil
        )
      )
    } else {
      reporter.error(fileImport.pos, s"no such file $scriptPath")
      None
    }
  }

  private def fromImport(
      base: AbsolutePath,
      qual: Term,
      fileImport: Name.Indeterminate,
      reporter: Reporter
  ): Option[FileImport] = {
    def loop(path: Path, parts: List[String]): Path =
      parts match {
        case Nil => path
        case "^" :: tail =>
          loop(path.getParent, tail)
        case "^^" :: tail =>
          loop(path.getParent.getParent(), tail)
        case "^^^" :: tail =>
          loop(path.getParent.getParent.getParent(), tail)
        case head :: tail =>
          loop(path.resolve(head), tail)
      }
    val parts = Term.Select(qual, Term.Name(fileImport.value)).syntax.split('.').toList
    val relativePath = parts.tail
    val packageName = parts.init.mkString(".")
    val objectName = parts.last
    val importedPath = loop(base.toNIO.getParent(), relativePath)
    val scriptPath = AbsolutePath(importedPath).resolveSibling(_ + ".sc")
    if (scriptPath.isFile) {
      val text = scriptPath.readText
      Some(
        FileImport(
          path = scriptPath,
          importName = MagicImport(fileImport.value, fileImport.pos),
          objectName = objectName,
          packageName = Some(packageName),
          source = text,
          dependencies = Nil,
          renames = Nil
        )
      )
    } else {
      reporter.error(fileImport.pos, s"no such file $scriptPath")
      None
    }
  }
}
