package tests.markdown

class AsyncSuite extends BaseMarkdownSuite {
  check(
    "await",
    """
      |```scala mdoc
      |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
      |Await.result(Future(1), Duration("500ms"))
      |```
    """.stripMargin,
    """|```scala
       |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
       |Await.result(Future(1), Duration("500ms"))
       |// res0: Int = 1
       |```
    """.stripMargin
  )

  checkError(
    "timeout",
    """
      |```scala mdoc
      |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
      |Await.result(Future(Thread.sleep(1000)), Duration("10ms"))
      |```
    """.stripMargin,
    """|error: timeout.md:4:1: Futures timed out after [10 milliseconds]
       |Await.result(Future(Thread.sleep(1000)), Duration("10ms"))
       |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       |java.lang.ExceptionInInitializerError
       |	at repl.Session$.app(timeout.md:3)
       |Caused by: java.util.concurrent.TimeoutException: Futures timed out after [10 milliseconds]
       |	at scala.concurrent.impl.Promise$DefaultPromise.ready(Promise.scala:259)
       |	at scala.concurrent.impl.Promise$DefaultPromise.result(Promise.scala:263)
       |	at scala.concurrent.Await$.$anonfun$result$1(package.scala:219)
       |	at scala.concurrent.BlockContext$DefaultBlockContext$.blockOn(BlockContext.scala:57)
       |	at scala.concurrent.Await$.result(package.scala:146)
       |	at repl.Session$App$.<init>(timeout.md:11)
       |	at repl.Session$App$.<clinit>(timeout.md)
       |	... 1 more
    """.stripMargin
  )
}
