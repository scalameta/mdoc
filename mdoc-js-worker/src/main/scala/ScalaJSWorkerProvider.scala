package mdoc.js.worker;

import mdoc.js.{interfaces => i}

import org.scalajs.{logging => sjslogging}

class ScalaJSWorkerProvider extends i.ScalajsWorkerProvider {
  def mapping(level: sjslogging.Level): i.LogLevel = {
    import i.LogLevel._
    import sjslogging.Level

    level match {
      case Level.Debug => Debug
      case Level.Info => Info
      case Level.Warn => Warning
      case Level.Error => Error
    }
  }
  def create(config: i.ScalajsConfig, logger: i.ScalajsLogger) = {
    val wrappedLogger = new sjslogging.Logger {
      def log(level: sjslogging.Level, message: => String) =
        logger.log(mapping(level), message)

      def trace(ex: => Throwable) =
        logger.trace(ex)
    }

    new ScalaJSWorker(config, wrappedLogger)
  }
}
