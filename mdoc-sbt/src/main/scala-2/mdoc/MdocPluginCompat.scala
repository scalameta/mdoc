package mdoc

import sbt._
import sbt.Keys._

trait MdocPluginCompat {
  protected final def getClasspathFiles(
      classpath: TaskKey[Classpath]
  ): Def.Initialize[Task[Seq[File]]] =
    Def.task(classpath.value.map(_.data))

  protected final def fileToFileRef(file: Def.Initialize[Task[File]]): Def.Initialize[Task[File]] =
    file
}
