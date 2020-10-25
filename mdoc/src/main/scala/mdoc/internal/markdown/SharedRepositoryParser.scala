/** This file is derived from the following Coursier sources, see NOTICE for license details.
  * https://github.com/coursier/coursier/blob/dfb04cab48a4beefa2be59fc71e3eff0493a5886/modules/coursier/shared/src/main/scala/coursier/Repositories.scala
  * https://github.com/coursier/coursier/blob/dfb04cab48a4beefa2be59fc71e3eff0493a5886/modules/coursier/shared/src/main/scala/coursier/internal/SharedRepositoryParser.scala
  */
package mdoc.internal.markdown

import scala.util.Try
import coursierapi.Repository
import coursierapi.IvyRepository
import coursierapi.MavenRepository
import coursierapi.error.CoursierError

object SharedRepositoryParser {

  def repository(s: String): Try[Repository] =
    Try {
      if (s == "central") {
        Repository.central()
      } else if (s.startsWith("sonatype:")) {
        Repositories.sonatype(s.stripPrefix("sonatype:"))
      } else if (s.startsWith("bintray:")) {
        val s0 = s.stripPrefix("bintray:")
        val id =
          if (s.contains("/")) s0
          else s0 + "/maven"
        Repositories.bintray(id)
      } else if (s.startsWith("bintray-ivy:")) {
        unsupportedIvy("bintray-ivy")
      } else if (s.startsWith("typesafe:ivy-")) {
        unsupportedIvy("typesafe:ivy-")
      } else if (s.startsWith("typesafe:")) {
        Repositories.typesafe(s.stripPrefix("typesafe:"))
      } else if (s.startsWith("sbt-maven:")) {
        Repositories.sbtMaven(s.stripPrefix("sbt-maven:"))
      } else if (s.startsWith("sbt-plugin:")) {
        unsupportedIvy("sbt-plugin")
      } else if (s.startsWith("ivy:")) {
        val s0 = s.stripPrefix("ivy:")
        val sepIdx = s0.indexOf('|')
        if (sepIdx < 0) IvyRepository.of(s0)
        else {
          val mainPart = s0.substring(0, sepIdx)
          val metadataPart = s0.substring(sepIdx + 1)
          IvyRepository.of(mainPart, metadataPart)
        }
      } else if (s == "jitpack") {
        Repositories.jitpack
      } else {
        MavenRepository.of(s)
      }
    }

  private def unsupportedIvy(what: String) =
    throw CoursierError.of(
      s"$what repositories are not supported. Please open a feature request to discuss adding support for $what repositories https://github.com/scalameta/mdoc/"
    )

  private object Repositories {
    def sonatype(name: String): MavenRepository =
      MavenRepository.of(s"https://oss.sonatype.org/content/repositories/$name")
    def bintray(id: String): MavenRepository =
      MavenRepository.of("https://dl.bintray.com/$id")
    def bintray(owner: String, repo: String): MavenRepository =
      bintray(s"$owner/$repo")
    def typesafe(id: String): MavenRepository =
      MavenRepository.of(s"https://repo.typesafe.com/typesafe/$id")
    def sbtMaven(id: String): MavenRepository =
      MavenRepository.of(s"https://repo.scala-sbt.org/scalasbt/maven-$id")
    def jitpack: MavenRepository =
      MavenRepository.of("https://jitpack.io")
  }
}
