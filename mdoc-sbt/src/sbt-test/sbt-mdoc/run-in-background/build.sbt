import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._
import MdocPlugin._

ThisBuild / scalaVersion := "2.12.17"

enablePlugins(MdocPlugin)

InputKey[Unit]("mdocBg") := Def.inputTaskDyn {
  validateSettings.value
  val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed
  val args = (mdocExtraArguments.value ++ parsed).mkString(" ")
  (Compile / bgRunMain).toTask(s" mdoc.SbtMain $args")
}.evaluated

TaskKey[Unit]("check") := {
  val commands = new LinkedBlockingQueue[String]()
  def sendInput(output: java.io.OutputStream): Unit = {
    val newLine = "\n".getBytes(StandardCharsets.UTF_8)
    try {
      while (true) {
        val command = commands.take()
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

  val output = new StringBuilder()
  def processOut(out: String): Unit = {
    if (out.endsWith("[info] started sbt server")) {
      commands.put("mdocBg --watch")
    } else if (out.endsWith("Waiting for file changes (press enter to interrupt)")) {
      // Wait for the input eating to start.
      Thread.sleep(3000)
      commands.put("show version")
    } else if (out.endsWith("[info] 0.1.0-SNAPSHOT")) {
      commands.put("")
      commands.put("exit")
    }
    println(s"[TEST] $out")
    output.append(out)
    output.append('\n')
  }

  val error = new StringBuilder()
  def processError(err: String): Unit = {
    println(s"[TEST ERROR] $err")
    error.append(err)
    output.append('\n')
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
  val deadline = Deadline.now + 30.seconds
  Future {
    while (p.isAlive()) {
      if (deadline.isOverdue()) {
        p.destroy()
      }
    }
  }
  p.exitValue()
  val code = p.exitValue()

  assert(code == 0, s"Expected exit code 0 but got $code")
}
