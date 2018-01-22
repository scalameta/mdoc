package fox

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Path
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import tut.Zed._
import tut._

object Repl {
  def compile(source: Path, options: Options): String = {
    val sw = new ByteArrayOutputStream()
    val io = Repl.tut(
      source.toFile,
      new PrintStream(sw),
      options.classpath,
      options.charset
    )
    io.unsafePerformIO()
    sw.toString(options.charset.name())
  }

  /**
    * Run the file through tut.
    *
    * @param in File to process
    * @param out File to write result to
    * @param opts Tut options
    * @return Ending state of tut after processing
    */
  def tut(
      in: File,
      out: OutputStream,
      opts: List[String],
      charset: Charset
  ): IO[TutState] =
    IO(new AnsiFilterStream(out)).using { filterStream =>
      IO(new Spigot(filterStream)).using { filterSpigot =>
        IO(new PrintStream(filterSpigot, true, charset.toString)).using {
          printStream =>
            IO(new OutputStreamWriter(printStream, charset)).using {
              streamWriter =>
                IO(new PrintWriter(streamWriter)).using { printWriter =>
                  (for {
                    interp <- newInterpreter(printWriter, opts)
                    state = TutState(
                      isCode = false,
                      Set(),
                      needsNL = false,
                      interp,
                      printWriter,
                      filterSpigot,
                      "",
                      err = false,
                      in,
                      opts
                    )
                    endSt <- Tut.file(in).exec(state)
                  } yield endSt).withOut(printStream)
                }
            }
        }
      }
    }

  private def newInterpreter(pw: PrintWriter, opts: List[String]): IO[IMain] = IO(
    new IMain(
      new Settings <| (_.embeddedDefaults[TutMain.type]) <| (_.processArguments(
        opts,
        processAll = true
      )),
      pw
    )
  )

}
