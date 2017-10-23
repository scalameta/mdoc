package fox

import caseapp._

object Main extends CaseApp[Options] {
  override def run(
      options: Options,
      remainingArgs: RemainingArgs
  ): Unit = {
    val runner = new Runner(options, Markdown.default, new Logger(System.out))
    runner.run()
    println(options.outPath.toString)
  }
}
