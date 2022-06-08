package tests.markdown

class ErrorSuite extends BaseMarkdownSuite {

  override def postProcessObtained: Map[Compat.ScalaVersion, String => String] =
    Map(
      Compat.All -> { old =>
        old.linesIterator
          .filterNot { line =>
            line.startsWith("did you mean") ||
            line.contains("(crash.md")
          }
          // Predef lines change between versions and platforms
          .map(line => line.replaceAll("Predef\\.scala:\\d\\d\\d", "Predef.scala:???"))
          .mkString("\n")
      }
    )
  private val tab = "\t"
  checkError(
    "crash",
    """
      |```scala mdoc
      |val x = 1
      |```
      |```scala mdoc
      |val y = 2
      |def crash() = ???
      |def z: Int = crash()
      |def safeMethod = 1 + 2
      |x + y + z
      |```
    """.stripMargin,
    """|error: crash.md:10:1: an implementation is missing
       |x + y + z
       |^^^^^^^^^
       |scala.NotImplementedError: an implementation is missing
       |	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:???)
       |""".stripMargin
  )

  checkError(
    "invalid-mod",
    """
      |```scala mdoc:foobaz
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |error: invalid-mod.md:2:15: Invalid mode 'foobaz'
      |```scala mdoc:foobaz
      |              ^^^^^^
    """.stripMargin
  )

  checkError(
    "silent",
    """
      |```scala mdoc:passthrough
      |import scala.util._
      |```
      |
      |```scala mdoc:fail
      |List(1)
      |```
    """.stripMargin,
    """|error: silent.md:7:1: Expected compile errors but program compiled successfully without errors
       |List(1)
       |^^^^^^^
       |""".stripMargin
  )

  checkError(
    "parse-error",
    """
      |```scala mdoc
      |val x =
      |```
    """.stripMargin,
    """
      |error: parse-error.md:3:8: illegal start of simple expression
      |val x =
      |       ^
    """.stripMargin
  )

  checkError(
    "not-member".tag(SkipScala3),
    """
      |```scala mdoc
      |List(1).len
      |```
    """.stripMargin,
    """|error: not-member.md:3:1: value len is not a member of List[Int]
       |List(1).len
       |^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "not-member-scala3".tag(OnlyScala3),
    """
      |```scala mdoc
      |List(1).len
      |```
    """.stripMargin,
    """|error: not-member-scala3.md:3:1:
       |value len is not a member of List[Int] - did you mean List[Int].min?
       |List(1).len
       |^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "already-defined".tag(SkipScala3),
    """
      |```scala mdoc
      |val x = 1
      |val x = 2
      |```
    """.stripMargin,
    """|error: already-defined.md:4:5: x is already defined as value x
       |val x = 2
       |    ^
    """.stripMargin
  )

  checkError(
    "already-defined-scala3".tag(OnlyScala3),
    """
      |```scala mdoc
      |val x = 1
      |val x = 2
      |```
    """.stripMargin,
    """|error: already-defined-scala3.md:4:5:
       |Double definition:
       |val x: Int in class MdocApp at line 7 and
       |val x: Int in class MdocApp at line 11
       |
       |val x = 2
       |    ^
    """.stripMargin
  )

  checkError(
    "yrangepos".tag(SkipScala3),
    """
      |```scala mdoc
      |List[Int]("".length.toString)
      |```
    """.stripMargin,
    """|error: yrangepos.md:3:11: type mismatch;
       | found   : String
       | required: Int
       |List[Int]("".length.toString)
       |          ^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "yrangepos-scala3".tag(OnlyScala3),
    """
      |```scala mdoc
      |List[Int]("".length.toString)
      |```
    """.stripMargin,
    """|error: yrangepos-scala3.md:3:11:
       |Found:    String
       |Required: Int
       |List[Int]("".length.toString)
       |          ^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "multimods-typo",
    """
      |```scala mdoc:reset:silen
      |List[Int]("".length.toString)
      |```
    """.stripMargin,
    """|error: multimods-typo.md:2:15: Invalid mode 'reset:silen'
       |```scala mdoc:reset:silen
       |              ^^^^^^^^^^^
    """.stripMargin
  )
}
