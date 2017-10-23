package scalamd

import caseapp._
import com.vladsch.flexmark.util.options.MutableDataSet

object Main extends CaseApp[ScalamdOptions] {
  override def run(
      options: ScalamdOptions,
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
