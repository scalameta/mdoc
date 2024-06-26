import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._

object SbtTest {

  def test(commands: TestCommand*) = {
    val commandsToSend = new LinkedBlockingQueue[String]()
    def sendInput(output: java.io.OutputStream): Unit = {
      val newLine = "\n".getBytes(StandardCharsets.UTF_8)
      try {
        while (true) {
          val command = commandsToSend.take()
          output.write(command.getBytes(StandardCharsets.UTF_8))
          output.write(newLine)
          output.flush()
        }
      } catch {
        case _: InterruptedException => // Ignore
      } finally {
        output.close()
      }
    }

    val commandQueue: mutable.Queue[TestCommand] = mutable.Queue(commands: _*)
    var expectedOutput: Option[String] = Some("[info] started sbt server")
    def processOut(out: String): Unit = {
      if (expectedOutput.forall(out.endsWith)) {
        if (commandQueue.nonEmpty) {
          val command = commandQueue.dequeue()
          Thread.sleep(command.delay.toMillis)
          commandsToSend.put(command.command)
          expectedOutput = command.expectedOutput
        }
      }
      println(s"[SbtTest] $out")
    }

    val error = new StringBuilder()
    def processError(err: String): Unit = {
      println(s"[SbtTest error] $err")
      error.append(err)
    }

    // TODO: Do we need the -Xmx setting and any other future options?
    val command = Seq(
      "sbt",
      s"-Dplugin.version=${sys.props("plugin.version")}",
      "--no-colors",
      "--supershell=never"
    )
    val logger = ProcessLogger(processOut, processError)
    val basicIO = BasicIO(withIn = false, logger)
    val io = new ProcessIO(sendInput, basicIO.processOutput, basicIO.processError)
    val p = command.run(io)

    val deadline = 30.seconds.fromNow
    Future {
      while (p.isAlive()) {
        if (deadline.isOverdue()) {
          p.destroy()
        }
      }
    }

    val code = p.exitValue()

    expectedOutput.foreach { expected =>
      throw new AssertionError(s"Expected to find output: $expected")
    }
    assert(code == 0, s"Expected exit code 0 but got $code")
  }
}
