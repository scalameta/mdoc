/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package mdoc.internal.markdown

import dotty.tools.io.AbstractFile

import java.net.{URL, URLConnection, URLStreamHandler}
import java.util.Collections

class AbstractFileClassLoader(val root: AbstractFile, parent: ClassLoader)
    extends ClassLoader(parent):

  /** Splits the given path using the given separator char, and finds the corresponding file through
    * subdirectories. Optionally adds the given suffix to the last component. This is intended to
    * make it easy to find files in formats such as "java/lang/Object" or "java.lang.Object".
    */
  final def lookupPath(
      path: String,
      separator: Char,
      lastSuffix: String = "",
      directory: Boolean = false
  ): Option[AbstractFile] =
    var file: AbstractFile = root
    var idx = 0
    var nextStepIdx = -1
    while
      nextStepIdx = path.indexOf(separator, idx)
      nextStepIdx != -1
    do
      file.lookupName(path.substring(idx, nextStepIdx), directory = true) match
        case null => return None
        case f =>
          file = f
          idx = nextStepIdx + 1
    Option(file.lookupName(path.substring(idx) + lastSuffix, directory = directory))
  end lookupPath

  // on JDK 20 the URL constructor we're using is deprecated,
  // but the recommended replacement, URL.of, doesn't exist on JDK 8
  @annotation.nowarn("cat=deprecation")
  override protected def findResource(name: String): URL | Null =
    lookupPath(name, '/') match
      case None => null
      case Some(file) => new URL(
          null,
          s"memory:${file.path}",
          new URLStreamHandler {
            override def openConnection(url: URL): URLConnection = new URLConnection(url) {
              override def connect() = ()
              override def getInputStream = file.input
            }
          }
        )
  override protected def findResources(name: String): java.util.Enumeration[URL] =
    findResource(name) match
      case null =>
        Collections.enumeration(Collections.emptyList[URL]) // Collections.emptyEnumeration[URL]
      case url => Collections.enumeration(Collections.singleton(url))

  override def findClass(name: String): Class[?] = {
    var file: AbstractFile | Null = root
    val pathParts = name.split("[./]").toList
    for (dirPart <- pathParts.init) {
      file = file.lookupName(dirPart, true)
      if (file == null) {
        throw new ClassNotFoundException(name)
      }
    }
    file = file.lookupName(pathParts.last + ".class", false)
    if (file == null) {
      throw new ClassNotFoundException(name)
    }
    val bytes = file.toByteArray
    defineClass(name, bytes, 0, bytes.length)
  }

  override def loadClass(name: String): Class[?] =
    try findClass(name)
    catch case _: ClassNotFoundException => super.loadClass(name)
end AbstractFileClassLoader
