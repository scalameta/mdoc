package scalamd

import caseapp._
import com.vladsch.flexmark.util.options.MutableDataSet

object Main extends CaseApp[Options] {
  override def run(
                      options: Options,
                      remainingArgs: RemainingArgs
  ): Unit = {
    val runner = new Runner(
      options,
      new MutableDataSet(),
      new Logger(System.out)

    )
    runner.run()
    println(options.outPath.toString)
  }
}
