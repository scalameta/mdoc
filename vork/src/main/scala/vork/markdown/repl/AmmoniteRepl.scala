package vork.markdown.repl

import java.io.File

import ammonite.interp.{Interpreter, Preprocessor}
import ammonite.ops.{Path, read}
import ammonite.repl._
import ammonite.runtime.{Frame, History, Storage}
import ammonite.util.Util.normalizeNewlines
import ammonite.util.{Res, _}

import scala.collection.mutable

// Copy pasted from Ammonite repl, original author Li Haoyi, license MIT
// https://github.com/lihaoyi/Ammonite/blob/233d18951875c3273eead3901ec445979db916ee/LICENSE
class AmmoniteRepl {
  var allOutput = ""
  def predef: (String, Option[ammonite.ops.Path]) = ("", None)

  val tempDir = ammonite.ops.Path(
    java.nio.file.Files.createTempDirectory("ammonite-tester")
  )

  import java.io.{ByteArrayOutputStream, PrintStream}

  val outBytes = new ByteArrayOutputStream
  val errBytes = new ByteArrayOutputStream
  val resBytes = new ByteArrayOutputStream
  def outString = new String(outBytes.toByteArray)
  def resString = new String(resBytes.toByteArray)

  val warningBuffer = mutable.Buffer.empty[String]
  val errorBuffer = mutable.Buffer.empty[String]
  val infoBuffer = mutable.Buffer.empty[String]
  val printer0 = Printer(
    new PrintStream(outBytes),
    new PrintStream(errBytes),
    new PrintStream(resBytes),
    x => warningBuffer.append(x + Util.newLine),
    x => errorBuffer.append(x + Util.newLine),
    x => infoBuffer.append(x + Util.newLine)
  )
  val storage = new Storage.Folder(tempDir)
  val frames = Ref(List(Frame.createInitial()))
  val sess0 = new SessionApiImpl(frames)

  var currentLine = 0
  val interp: Interpreter = try {
    new Interpreter(
      printer0,
      storage = storage,
      wd = ammonite.ops.pwd,
      basePredefs = Seq(
        PredefInfo(
          Name("defaultPredef"),
          ammonite.main.Defaults.replPredef + ammonite.main.Defaults.predefString,
          true,
          None
        ),
        PredefInfo(Name("testPredef"), predef._1, false, predef._2)
      ),
      customPredefs = Seq(),
      extraBridges = Seq(
        (
          "ammonite.repl.ReplBridge",
          "repl",
          new ReplApiImpl {
            def replArgs0 = Vector.empty[Bind[_]]
            def printer = printer0

            def sess = sess0
            val prompt = Ref("@")
            val frontEnd = Ref[FrontEnd](null)
            def lastException: Throwable = null
            def fullHistory = storage.fullHistory()
            def history = new History(Vector())
            val colors = Ref(Colors.BlackWhite)
            def newCompiler() = interp.compilerManager.init(force = true)
            def compiler = interp.compilerManager.compiler.compiler
            def fullImports = interp.predefImports ++ imports
            def imports = interp.frameImports
            def width = 80
            def height = 80

            object load extends ReplLoad with (String => Unit) {

              def apply(line: String) = {
                interp.processExec(line, currentLine, () => currentLine += 1) match {
                  case Res.Failure(s) => throw new CompilationError(s)
                  case Res.Exception(t, s) => throw t
                  case _ =>
                }
              }

              def exec(file: Path): Unit = {
                interp.watch(file)
                apply(normalizeNewlines(read(file)))
              }
            }
          }
        )
      ),
      colors = Ref(Colors.BlackWhite),
      getFrame = () => frames().head,
      createFrame = () => { val f = sess0.childFrame(frames().head); frames() = f :: frames(); f },
      replCodeWrapper = Preprocessor.CodeWrapper,
      scriptCodeWrapper = Preprocessor.CodeWrapper
    )

  } catch {
    case e: Throwable =>
      println(infoBuffer.mkString)
      println(outString)
      println(resString)
      println(warningBuffer.mkString)
      println(errorBuffer.mkString)
      throw e
  }

  for ((error, _) <- interp.initializePredef()) {
    val (msgOpt, causeOpt) = error match {
      case r: Res.Exception => (Some(r.msg), Some(r.t))
      case r: Res.Failure => (Some(r.msg), None)
      case _ => (None, None)
    }

    println(infoBuffer.mkString)
    println(outString)
    println(resString)
    println(warningBuffer.mkString)
    println(errorBuffer.mkString)
    throw new Exception(
      s"Error during predef initialization${msgOpt.fold("")(": " + _)}",
      causeOpt.orNull
    )
  }

  def loadClasspath(classpath: String): Unit = {
    if (!classpath.isEmpty) {
      val files = classpath.split(File.pathSeparator).map(new File(_))
      interp.headFrame.addClasspath(files)
    }
  }

  def run(input: String): RunResult = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()
    val result = Console.withOut(stdout) {
      Console.withErr(stderr) {
        runImpl(input, this.currentLine)
      }
    }
    result.copy(
      stdout = stdout.toString(),
      stderr = stderr.toString()
    )
  }

  def runImpl(input: String, index: Int): RunResult = {
    outBytes.reset()
    resBytes.reset()
    warningBuffer.clear()
    errorBuffer.clear()
    infoBuffer.clear()
    val splitted = ammonite.interp.Parsers.split(input).get.get.value
    val processed = interp.processLine(
      input,
      splitted,
      index,
      false,
      () => currentLine += 1
    )
    processed match {
      case Res.Failure(s) => printer0.error(s)
      case Res.Exception(throwable, msg) =>
        printer0.error(
          Repl.showException(throwable, fansi.Attrs.Empty, fansi.Attrs.Empty, fansi.Attrs.Empty)
        )

      case _ =>
    }
    Repl.handleOutput(interp, processed)
    RunResult(
      processed,
      outString,
      resString,
      warningBuffer.mkString,
      errorBuffer.mkString,
      infoBuffer.mkString,
      "",
      ""
    )
  }
}

case class RunResult(
    evaluated: Res[Evaluated],
    processed: String,
    outString: String,
    warning: String,
    error: String,
    info: String,
    stdout: String,
    stderr: String
) {}
