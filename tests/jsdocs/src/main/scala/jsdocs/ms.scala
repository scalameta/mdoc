package jsdocs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("ms", JSImport.Namespace)
object ms extends js.Object {
  // Facade for the npm package `ms` https://www.npmjs.com/package/ms
  def apply(n: Double): String = js.native
}
