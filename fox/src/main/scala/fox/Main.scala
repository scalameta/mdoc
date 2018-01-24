package fox

import caseapp._

object Main extends CaseApp[Options] {
  override def run(
      options: Options,
      remainingArgs: RemainingArgs
  ): Unit = {
    val markdown = Markdown.default(options)
    val runner = new Processor(options, markdown, new Logger(System.out))
    runner.run()
    println(options.outPath.toString)
  }
}
