package fox

import scala.util.control.NoStackTrace
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.Configured

object Cli {
  def main(args: Array[String]): Unit = {
    val parsed = Conf
      .parseCliArgs[Options](args.toList)
      .andThen(_.as[Options])
    parsed match {
      case Configured.NotOk(error) =>
        throw new Exception(error.toString()) with NoStackTrace
      case Configured.Ok(options) =>
        val markdown = Markdown.default(options)
        val runner = new Processor(options, markdown, new Logger(System.out))
        runner.run()
        println(options.outPath.toString)
    }
  }
}
