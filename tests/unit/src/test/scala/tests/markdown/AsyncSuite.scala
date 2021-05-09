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
       |java.util.concurrent.TimeoutException: Futures timed out after [10 milliseconds]
       |	at scala.concurrent.impl.Promise$DefaultPromise.ready(Promise.scala:259)
       |	at scala.concurrent.impl.Promise$DefaultPromise.result(Promise.scala:263)
       |	at scala.concurrent.Await$.$anonfun$result$1(package.scala:223)
       |	at scala.concurrent.BlockContext$DefaultBlockContext$.blockOn(BlockContext.scala:57)
       |	at scala.concurrent.Await$.result(package.scala:146)
       |	at repl.MdocSession$App.<init>(timeout.md:11)
       |	at repl.MdocSession$.app(timeout.md:3)
       |""".stripMargin,
    compat = Map(
      "2.11" ->
        """|error: timeout.md:4:1: Futures timed out after [10 milliseconds]
           |Await.result(Future(Thread.sleep(1000)), Duration("10ms"))
           |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
           |java.util.concurrent.TimeoutException: Futures timed out after [10 milliseconds]
           |	at scala.concurrent.impl.Promise$DefaultPromise.ready(Promise.scala:223)
           |	at scala.concurrent.impl.Promise$DefaultPromise.result(Promise.scala:227)
           |	at scala.concurrent.Await$$anonfun$result$1.apply(package.scala:190)
           |	at scala.concurrent.BlockContext$DefaultBlockContext$.blockOn(BlockContext.scala:53)
           |	at scala.concurrent.Await$.result(package.scala:190)
           |	at repl.MdocSession$App.<init>(timeout.md:11)
           |	at repl.MdocSession$.app(timeout.md:3)
           |""".stripMargin,
      "2.13" ->
        """|error: timeout.md:4:1: Future timed out after [10 milliseconds]
           |Await.result(Future(Thread.sleep(1000)), Duration("10ms"))
           |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
           |java.util.concurrent.TimeoutException: Future timed out after [10 milliseconds]
           |	at scala.concurrent.impl.Promise$DefaultPromise.tryAwait0(Promise.scala:212)
           |	at scala.concurrent.impl.Promise$DefaultPromise.result(Promise.scala:225)
           |	at scala.concurrent.Await$.$anonfun$result$1(package.scala:201)
           |	at scala.concurrent.BlockContext$DefaultBlockContext$.blockOn(BlockContext.scala:62)
           |	at scala.concurrent.Await$.result(package.scala:124)
           |	at repl.MdocSession$App.<init>(timeout.md:11)
           |	at repl.MdocSession$.app(timeout.md:3)
           |""".stripMargin,
      "3.0" ->
        """|error: timeout.md:4:1: Future timed out after [10 milliseconds]
           |Await.result(Future(Thread.sleep(1000)), Duration("10ms"))
           |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
           |java.util.concurrent.TimeoutException: Future timed out after [10 milliseconds]
           |	at scala.concurrent.impl.Promise$DefaultPromise.tryAwait0(Promise.scala:212)
           |	at scala.concurrent.impl.Promise$DefaultPromise.result(Promise.scala:225)
           |	at scala.concurrent.Await$.$anonfun$result$1(package.scala:201)
           |	at scala.concurrent.BlockContext$DefaultBlockContext$.blockOn(BlockContext.scala:62)
           |	at scala.concurrent.Await$.result(package.scala:124)
           |	at repl.MdocSession$App.<init>(timeout.md:11)
           |	at repl.MdocSession$.app(timeout.md:3)
           |""".stripMargin
    )
  )

  check(
    "reset-class".flaky,
    """
      |```scala mdoc
      |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
      |val x = 1
      |Await.result(Future(x), Duration("100ms"))
      |```
    """.stripMargin,
    """|
       |```scala
       |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
       |val x = 1
       |// x: Int = 1
       |Await.result(Future(x), Duration("100ms"))
       |// res0: Int = 1
       |```
       |
       |""".stripMargin
  )

  check(
    "println".flaky,
    """
      |```scala mdoc
      |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
      |val done = scala.concurrent.Promise[Unit]()
      |global.execute(new Runnable {
      |  override def run(): Unit = {
      |    Thread.sleep(20)
      |    println("Hello from other thread!")
      |    done.success(())
      |  }
      |})
      |Await.result(done.future, Duration("100ms"))
      |```
    """.stripMargin,
    """|```scala
       |import scala.concurrent._, duration._, ExecutionContext.Implicits.global
       |val done = scala.concurrent.Promise[Unit]()
       |// done: Promise[Unit] = Future(Success(()))
       |global.execute(new Runnable {
       |  override def run(): Unit = {
       |    Thread.sleep(20)
       |    println("Hello from other thread!")
       |    done.success(())
       |  }
       |})
       |Await.result(done.future, Duration("100ms"))
       |// Hello from other thread!
       |```
       |""".stripMargin
  )
}
