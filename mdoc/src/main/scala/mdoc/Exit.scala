package mdoc

import mdoc.internal.cli.{Exit => CliExit}

final class Exit private[mdoc] (val exit: CliExit) {
  def merge(other: Exit): Exit = Exit(exit.merge(other.exit))
  def isSuccess: Boolean = exit == CliExit.success
  def isError: Boolean = exit == CliExit.error
}

object Exit {
  private[mdoc] def apply(exit: CliExit): Exit = {
    new Exit(exit)
  }
  def apply(): Exit = {
    Exit(CliExit.success)
  }
}
