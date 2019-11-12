package tests.markdown

class LiteralTypesSuite extends BaseMarkdownSuite {

  check(
    "val declaration",
    """
      |```scala mdoc
      |val one: 1 = 1
      |```
    """.stripMargin,
    """|```scala
       |val one: 1 = 1
       |// one: Int = 1
       |```
    """.stripMargin
  )

  check(
    "param type, type arg",
    """
      |```scala mdoc
      |def foo(x: 1): Option[1] = Some(x)
      |```
    """.stripMargin,
    """|```scala
       |def foo(x: 1): Option[1] = Some(x)
       |```
    """.stripMargin
  )

  check(
    "type parameter bound",
    """
      |```scala mdoc
      |def bar[T <: 1](t: T): T = t
      |```
    """.stripMargin,
    """|```scala
       |def bar[T <: 1](t: T): T = t
       |```
    """.stripMargin
  )

  check(
    "type ascription",
    """
      |```scala mdoc
      |def foo(x: Int): Int = x
      |foo(1: 1)
      |```
    """.stripMargin,
    """|```scala
       |def foo(x: Int): Int = x
       |foo(1: 1)
       |// res0: Int = 1
       |```
    """.stripMargin
  )

  check(
    "narrowing",
    """
      |```scala mdoc
      |def narrower[T <: Singleton](t: T): T {} = t
      |narrower(42)
      |```
    """.stripMargin,
    """|```scala
       |def narrower[T <: Singleton](t: T): T {} = t
       |narrower(42)
       |// res0: 42 = 42
       |```
    """.stripMargin
  )

}
