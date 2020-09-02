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
import scala.collection.JavaConverters._

/* Not implemented for Scala3 - left for compilation sake only.
 * Can be removed once the project is updated to use Scalameta parser for Scala 3 */
final case class FileImport() {
  def toInterface: ImportedScriptFile = {
    new mdoc.interfaces.ImportedScriptFile {
      def path(): Path = ???
      def packageName(): String = ???
      def objectName(): String = ???
      def instrumentedSource(): String = ???
      def originalSource(): String = ???
      def files(): java.util.List[ImportedScriptFile] = ???
    }
  }
}
