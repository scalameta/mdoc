package tests.markdown

// TODO Replicate these types of tests for inline.
//    Also: why does this only exists for Scala-2?
class CompileOnlySuite extends BaseMarkdownSuite {
  check(
    "compile-only",
    """
      |```scala mdoc:compile-only
      |println(System.in.read())
      |```
    """.stripMargin,
    """|```scala
       |println(System.in.read())
       |```
    """.stripMargin
  )

  check(
    "reuse",
    """
      |```scala mdoc
      |val message = "Enter: "
      |```
      |```scala mdoc:compile-only
      |println(message)
      |println(System.in.read())
      |```
      |```scala mdoc
      |println(message)
      |```
    """.stripMargin,
    // assert it's possible to reference variables from non-compile-only blocks
    """|```scala
       |val message = "Enter: "
       |// message: String = "Enter: "
       |```
       |```scala
       |println(message)
       |println(System.in.read())
       |```
       |```scala
       |println(message)
       |// Enter:
       |```
    """.stripMargin
  )

  checkError(
    "no-reuse",
    """
      |```scala mdoc:compile-only
      |val message = "Enter: "
      |```
      |```scala mdoc
      |println(message)
      |```
    """.stripMargin,
    // assert it's not possible to reference variables from compile-only blocks
    """|error: no-reuse.md:6:9: not found: value message
       |println(message)
       |        ^^^^^^^
    """.stripMargin
  )

  checkError(
    "error",
    """
      |```scala mdoc:compile-only
      |val x: String = 42
      |```
    """.stripMargin,
    // Validate that compile errors are reported and fail the build.
    """|error: error.md:3:17: type mismatch;
       | found   : Int(42)
       | required: String
       |val x: String = 42
       |                ^^
    """.stripMargin
  )

}
