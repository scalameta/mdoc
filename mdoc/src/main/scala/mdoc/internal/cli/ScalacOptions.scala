package mdoc.internal.cli

object ScalacOptions {

  private val OptionSeparator = "\n"

  def parse(s: String): List[String] =
    if (s.isEmpty) Nil
    else if (s.contains(OptionSeparator))
      s.split(OptionSeparator).filter(_.nonEmpty).toList
    else
      s.split("\\s+").filter(_.nonEmpty).toList

  def serialize(options: Seq[String]): String =
    options.mkString(OptionSeparator)
}
