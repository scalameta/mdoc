package jsdocs

class Progress {
  var i = -1
  val max = 20
  def tick(_unused: Int): String = {
    i += 1
    val n = i % max + 1
    val hash = "#" * n
    val space = " " * (max - n)
    val percentage = ((n.toDouble / max) * 100).toInt
    "<pre><code>" + hash + "🚀" + space + s"$percentage%" + "</code></pre>"
  }
}
