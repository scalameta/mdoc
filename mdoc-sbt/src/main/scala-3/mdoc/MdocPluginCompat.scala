package mdoc

import sbt.*
import sbt.Keys.*

trait MdocPluginCompat {
  export sbt.util.CacheImplicits.given

  protected final def getClasspathFiles(
      classpath: TaskKey[Classpath]
  ): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      val converter = fileConverter.value
      classpath.value.map(f => converter.toPath(f.data).toFile)
    }

  protected final def fileToFileRef(
      file: Def.Initialize[Task[File]]
  ): Def.Initialize[Task[HashedVirtualFileRef]] =
    Def.task(fileConverter.value.toVirtualFile(file.value.toPath))
}
