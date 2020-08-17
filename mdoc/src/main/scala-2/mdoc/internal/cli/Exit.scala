package mdoc.internal.cli

sealed abstract class Exit extends Product with Serializable {
  def merge(other: Exit): Exit =
    this match {
      case Exit.Success => other
      case Exit.Error => this
    }
  def isSuccess: Boolean = this == Exit.Success
  def isError: Boolean = this == Exit.Error
}
object Exit {
  def success: Exit = Success
  def error: Exit = Error
  private case object Success extends Exit
  private case object Error extends Exit
}
