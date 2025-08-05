package tests.imports

import tests.markdown.BaseMarkdownSuite
import tests.markdown.Compat
import scala.util.Properties
import tests.BuildInfo

class DependencySuite extends BaseMarkdownSuite {
  val userHome = System.getProperty("user.home")
  override def postProcessObtained: Map[Compat.ScalaVersion, String => String] =
    Map(
      Compat.All -> { old =>
        old.linesIterator
          .map {
            case line if line.contains(userHome) =>
              "<redacted user.home>"
            case line => line
          }
          .mkString("\n")
      }
    )

  List(
    "$dep" -> "import $dep.`org.dhallj::dhall-scala:0.3.0`, org.dhallj.syntax._",
    "$ivy" -> "import $ivy.`org.dhallj::dhall-scala:0.3.0`, org.dhallj.syntax._",
    "using-dep" -> """|//> using dep org.dhallj::dhall-scala:0.3.0
                      |import org.dhallj.syntax._
                      |""".stripMargin
  ).foreach { case (name, dep) =>
    check(
      name.tag(OnlyScala213),
      s"""
         |```scala mdoc
         |$dep
         |"\\\\(n: Natural) -> [n + 0, n + 1, 1 + 1]".parseExpr
         |```
         | """.stripMargin,
      s"""|```scala
          |$dep
          |"\\\\(n: Natural) -> [n + 0, n + 1, 1 + 1]".parseExpr
          |// res0: Either[org.dhallj.core.DhallException.ParsingFailure, org.dhallj.core.Expr] = Right(
          |//   value = λ(n : Natural) → [n + 0, n + 1, 1 + 1]
          |// )
          |```
          |""".stripMargin
    )
  }

  checkError(
    "unknown".tag(OnlyScala213),
    s"""
       |```scala mdoc
       |//> using depaa org.dhallj::dhall-scala:0.3.0
       |import org.dhallj.syntax._
       |"\\\\(n: Natural) -> [n + 0, n + 1, 1 + 1]".parseExpr
       |```
       | """.stripMargin,
    s"""|warning: unknown.md:3:11: Unknown directive: depaa
        |//> using depaa org.dhallj::dhall-scala:0.3.0
        |          ^
        |error: unknown.md:4:8: object dhallj is not a member of package org
        |import org.dhallj.syntax._
        |       ^^^^^^^^^^
        |error: unknown.md:5:1: value parseExpr is not a member of String
        |"\\\\(n: Natural) -> [n + 0, n + 1, 1 + 1]".parseExpr
        |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        |""".stripMargin
  )

  // The test is unrelabile because it depends on the snapshots repository
  check(
    "repo".ignore,
    """
      |```scala mdoc
      |import $repo.`https://central.sonatype.com/repository/maven-snapshots/`
      |import $dep.`org.scalameta:metals_2.13:0.11.10+1-4aa438b0-SNAPSHOT`
      |scala.meta.internal.metals.ScalaVersions.isScala3Milestone("3.0.0")
      |```
      | """.stripMargin,
    """|```scala
       |import $repo.`https://central.sonatype.com/repository/maven-snapshots/`
       |import $dep.`org.scalameta:metals_2.13:0.11.10+1-4aa438b0-SNAPSHOT`
       |scala.meta.internal.metals.ScalaVersions.isScala3Milestone("3.0.0")
       |// res0: Boolean = false
       |```
       |""".stripMargin
  )

  check(
    "repo-using".tag(OnlyScala213),
    """
      |```scala mdoc
      |//> using repo https://oss.sonatype.org/content/repositories/snapshots
      |//> using dep org.scalameta:metals_2.13:0.11.10+1-4aa438b0-SNAPSHOT
      |scala.meta.internal.metals.ScalaVersions.isScala3Milestone("3.0.0")
      |```
      | """.stripMargin,
    """|```scala
       |//> using repo https://oss.sonatype.org/content/repositories/snapshots
       |//> using dep org.scalameta:metals_2.13:0.11.10+1-4aa438b0-SNAPSHOT
       |scala.meta.internal.metals.ScalaVersions.isScala3Milestone("3.0.0")
       |// res0: Boolean = false
       |```
       |""".stripMargin
  )

  checkError(
    "ammonite".tag(OnlyScala213),
    """
      |```scala mdoc
      |import $repo.`sbt-plugin:foobar`
      |println(42)
      |```
      | """.stripMargin,
    s"""|error: ammonite.md:3:14: sbt-plugin repositories are not supported. Please open a feature request to discuss adding support for sbt-plugin repositories https://github.com/scalameta/mdoc/
        |import $$repo.`sbt-plugin:foobar`
        |             ^^^^^^^^^^^^^^^^^^^
        |""".stripMargin
  )

  checkError(
    "using".tag(OnlyScala213),
    """
      |```scala mdoc
      |//> using repo sbt-plugin:foobar
      |println(42)
      |```
      | """.stripMargin,
    s"""|error: using.md:3:11: sbt-plugin repositories are not supported. Please open a feature request to discuss adding support for sbt-plugin repositories https://github.com/scalameta/mdoc/
        |//> using repo sbt-plugin:foobar
        |          ^
        |""".stripMargin
  )

  checkError(
    "dep-error".tag(OnlyScala213),
    """
      |```scala mdoc
      |import $dep.`org.scalameta::mmunit:2.3.4`, $dep.`org.scalameta:foobar:1.2.1`
      |import $dep.`org.scalameta:::not-exists:2.3.4`
      |import $dep.`org.scalameta::munit:0.7.5` // resolves OK
      |println(42)
      |```
      | """.stripMargin,
    s"""|error: dep-error.md:4:13: Error downloading org.scalameta:not-exists_${BuildInfo.scalaVersion}:2.3.4
        |<redacted user.home>
        |  not found: https://repo1.maven.org/maven2/org/scalameta/not-exists_${BuildInfo.scalaVersion}/2.3.4/not-exists_${BuildInfo.scalaVersion}-2.3.4.pom
        |import $$dep.`org.scalameta:::not-exists:2.3.4`
        |            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        |error: dep-error.md:3:49: Error downloading org.scalameta:foobar:1.2.1
        |<redacted user.home>
        |  not found: https://repo1.maven.org/maven2/org/scalameta/foobar/1.2.1/foobar-1.2.1.pom
        |import $$dep.`org.scalameta::mmunit:2.3.4`, $$dep.`org.scalameta:foobar:1.2.1`
        |                                                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        |error: dep-error.md:3:13: Error downloading org.scalameta:mmunit_2.13:2.3.4
        |<redacted user.home>
        |  not found: https://repo1.maven.org/maven2/org/scalameta/mmunit_2.13/2.3.4/mmunit_2.13-2.3.4.pom
        |import $$dep.`org.scalameta::mmunit:2.3.4`, $$dep.`org.scalameta:foobar:1.2.1`
        |            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        |""".stripMargin
  )

  checkError(
    "dep-error-using".tag(OnlyScala213),
    """
      |```scala mdoc
      |//> using dep org.scalameta::mmunit:2.3.4 org.scalameta:foobar:1.2.1
      |//> using dep org.scalameta:::not-exists:2.3.4
      |//> using dep org.scalameta::munit:0.7.5 // resolves OK
      |println(42)
      |```
      | """.stripMargin,
    s"""|error: dep-error-using.md:4:11: Error downloading org.scalameta:not-exists_2.13.16:2.3.4
        |<redacted user.home>
        |  not found: https://repo1.maven.org/maven2/org/scalameta/not-exists_2.13.16/2.3.4/not-exists_2.13.16-2.3.4.pom
        |//> using dep org.scalameta:::not-exists:2.3.4
        |          ^
        |error: dep-error-using.md:3:11: Error downloading org.scalameta:foobar:1.2.1
        |<redacted user.home>
        |  not found: https://repo1.maven.org/maven2/org/scalameta/foobar/1.2.1/foobar-1.2.1.pom
        |//> using dep org.scalameta::mmunit:2.3.4 org.scalameta:foobar:1.2.1
        |          ^
        |error: dep-error-using.md:3:11: Error downloading org.scalameta:mmunit_2.13:2.3.4
        |<redacted user.home>
        |  not found: https://repo1.maven.org/maven2/org/scalameta/mmunit_2.13/2.3.4/mmunit_2.13-2.3.4.pom
        |//> using dep org.scalameta::mmunit:2.3.4 org.scalameta:foobar:1.2.1
        |          ^
        |""".stripMargin
  )

  checkError(
    "dep-syntax-error".tag(OnlyScala213),
    """
      |```scala mdoc
      |import $dep.`org.scalameta:no-version`
      |import $dep.`org.scalameta::has-version:1.0.0`
      |println(42)
      |```
      | """.stripMargin,
    """|error: dep-syntax-error.md:3:13: invalid dependency. To fix this error, use the format `$ORGANIZATION:$ARTIFACT:$NAME`.
       |import $dep.`org.scalameta:no-version`
       |            ^^^^^^^^^^^^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  checkError(
    "dep-syntax-error-using".tag(OnlyScala213),
    """
      |```scala mdoc
      |//> using dep org.scalameta:no-version
      |//> using dep org.scalameta::has-version:1.0.0
      |println(42)
      |```
      | """.stripMargin,
    """|error: dep-syntax-error-using.md:3:11: invalid dependency. To fix this error, use the format `$ORGANIZATION:$ARTIFACT:$NAME`.
       |//> using dep org.scalameta:no-version
       |          ^
       |""".stripMargin
  )

}
