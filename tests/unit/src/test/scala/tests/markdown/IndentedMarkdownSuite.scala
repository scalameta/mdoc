package tests.markdown

class IndentedMarkdownSuite extends BaseMarkdownSuite {

  check(
    "2 whitespace",
    """
      |  ```scala
      |  val msg = "Hello!"
      |  println(msg)
      |  ```
    """.stripMargin,
    """
      |  ```scala
      |  val msg = "Hello!"
      |  println(msg)
      |  ```
    """.stripMargin
  )

  check(
    "4 whitespace",
    """
      |    ```scala
      |    val msg = "Hello!"
      |    println(msg)
      |    ```
    """.stripMargin,
    """
      |    ```scala
      |    val msg = "Hello!"
      |    println(msg)
      |    ```
    """.stripMargin
  )

  check(
    "tab",
    """
      | ```scala
      | val msg = "Hello!"
      | println(msg)
      | ```
    """.stripMargin,
    """
      | ```scala
      | val msg = "Hello!"
      | println(msg)
      | ```
    """.stripMargin
  )

  check(
    "4 whitespace, tagged",
    """
      |:   ```scala
      |    val msg = "Hello!"
      |    println(msg)
      |    ```
    """.stripMargin,
    """
      |:   ```scala
      |    val msg = "Hello!"
      |    println(msg)
      |    ```
    """.stripMargin
  )

  check(
    "4 whitespace, tagged, mdoc",
    """
      |:   ```scala mdoc
      |    val msg = "Hello!"
      |    println(msg)
      |    ```
    """.stripMargin,
    """
      |:   ```scala
      |    val msg = "Hello!"
      |    // msg: String = "Hello!"
      |    println(msg)
      |    // Hello!
      |    ```
    """.stripMargin
  )
}
