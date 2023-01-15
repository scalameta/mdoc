import scala.concurrent.duration._

/** @param command
  *   the command to send
  * @param expectedOutput
  *   expected output of the command
  * @param delay
  *   time to wait before sending the command
  */
final case class TestCommand(command: String, expectedOutput: Option[String], delay: FiniteDuration)

object TestCommand {
  def apply(command: String, expectedOutput: String, delay: FiniteDuration): TestCommand =
    TestCommand(command, Some(expectedOutput), delay)

  def apply(command: String, expectedOutput: String): TestCommand =
    TestCommand(command, Some(expectedOutput), Duration.Zero)

  def apply(command: String, delay: FiniteDuration): TestCommand =
    TestCommand(command, None, delay)

  def apply(command: String): TestCommand =
    TestCommand(command, None, Duration.Zero)
}
