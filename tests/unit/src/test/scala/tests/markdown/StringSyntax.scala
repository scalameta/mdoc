package tests.markdown

object StringSyntax {
  implicit class XtensionStringSyntax(str: String) {
    def triplequoted: String = str.replaceAllLiterally("'''", "\"\"\"")
    def trimLineEnds: String = {
      str.linesIterator.map(_.trimEnd).mkString("\n")
    }
    def trimEnd: String = {
      var len = str.length
      val st = 0
      while ((st < len) && (str.charAt(len - 1) == ' ')) len -= 1
      if (len == str.length) str
      else str.substring(0, len)
    }
  }

}
