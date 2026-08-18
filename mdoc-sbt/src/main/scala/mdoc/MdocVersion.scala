package mdoc

import sbt._
import sbt.librarymanagement.CrossVersion

object MdocVersion {

  /** Resolve the mdoc (or mdoc-js) module for a user's Scala version.
    *
    * Supported compilers get the current release with `CrossVersion.full`. Everything else falls
    * back to the last binary-published mdoc.
    */
  def module(
      artifact: String,
      scalaVersion: String,
      mdocVersion: String,
      lastBinaryVersion: String,
      supportedScalaVersions: Set[String]
  ): ModuleID = {
    val org = "org.scalameta"
    if (useFullVersion(scalaVersion, supportedScalaVersions))
      (org %% artifact % mdocVersion).cross(CrossVersion.full)
    else
      org %% artifact % lastBinaryVersion
  }

  def artifactVersion(
      scalaVersion: String,
      mdocVersion: String,
      lastBinaryVersion: String,
      supportedScalaVersions: Set[String]
  ): String =
    if (useFullVersion(scalaVersion, supportedScalaVersions)) mdocVersion
    else lastBinaryVersion

  /** Use `CrossVersion.full` when the supported list is empty (properties not loaded, e.g. this
    * repository's meta-build) or when the compiler is one we publish for. Only known-unsupported
    * compilers fall back to the last binary-published mdoc.
    */
  def useFullVersion(scalaVersion: String, supportedScalaVersions: Set[String]): Boolean =
    supportedScalaVersions.isEmpty || supportedScalaVersions.contains(scalaVersion)
}
