package mdoc.internal.worksheets

import java.{util => ju}
import mdoc.{interfaces => i}
import java.nio.file.Path

final case class ImportedScriptFile(
    val path: Path,
    val packageName: String,
    val objectName: String,
    val instrumentedSource: String,
    val originalSource: String,
    val files: ju.List[i.ImportedScriptFile]
) extends i.ImportedScriptFile
