package mdoc.internal.cli

object Messages {
  def count(what: String, number: Int): String = {
    if (number == 1) s"$number $what"
    else s"$number ${what}s"
  }
}
