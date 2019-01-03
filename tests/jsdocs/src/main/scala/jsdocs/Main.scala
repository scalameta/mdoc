package jsdocs

object Main {
  def main(args: Array[String]): Unit = {
    // Important, the main function must use the Scala.js facade in order for
    // the `ms` npm library to be available from mdoc:js code fences.
    ms(6000)
  }
}
