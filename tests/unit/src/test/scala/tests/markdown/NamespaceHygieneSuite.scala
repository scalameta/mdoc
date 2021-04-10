package tests.markdown

class NamespaceHygieneSuite extends BaseMarkdownSuite {

  check(
    "ok to import something called Session",
    """|```scala mdoc
       |import scala.util.{ Random => Session }
       |val x = Session.nextInt(1)
       |```""".stripMargin,
    """|```scala
       |import scala.util.{ Random => Session }
       |val x = Session.nextInt(1)
       |// x: Int = 0
       |```""".stripMargin
  )

  checkError(
    "cannot import something called MdocSession",
    """|```scala mdoc
       |import scala.util.{ Random => MdocSession }
       |val x = MdocSession.nextInt(1)
       |```""".stripMargin,
    """|error: cannot import something called MdocSession.md:3:9: reference to MdocSession is ambiguous;
       |it is both defined in package repl and imported subsequently by
       |import scala.util.{Random=>MdocSession}
       |val x = MdocSession.nextInt(1)
       |        ^^^^^^^^^^^
       |""".stripMargin,
    compat = Map(
      "3.0" ->
        """
          |error: cannot import something called MdocSession.md:3:9:
          |Reference to MdocSession is ambiguous,
          |it is both defined in package repl
          |and imported by name subsequently by import util.{...}
        """.stripMargin
    )
  )

}
