package mdoc.internal.markdown

/** Generates fresh identifiers for instrumented code.
  *
  * @param reserved
  *   Names that already appear as top-level definitions in the user source. Fresh names skip these
  *   so synthetic `resN` binders do not collide with user bindings such as `val res2 = 1` (see
  *   scalameta/metals#5680).
  */
class Gensym(reserved: Set[String] = Set.empty) {
  private var counter = 0
  def reset(): Unit = {
    counter = 0
  }
  def fresh(prefix: String, suffix: String = ""): String = {
    var name = s"$prefix$counter$suffix"
    counter += 1
    while (reserved.contains(name)) {
      name = s"$prefix$counter$suffix"
      counter += 1
    }
    name
  }
}
