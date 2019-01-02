package mdoc.internal.markdown

class Gensym() {
  private var counter = 0
  def reset(): Unit = {
    counter = 0
  }
  def fresh(prefix: String, suffix: String = ""): String = {
    val name = s"$prefix$counter$suffix"
    counter += 1
    name
  }
}
