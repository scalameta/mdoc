package tests.markdown

import mdoc.internal.document.MdocExceptions
import org.scalatest.funsuite.AnyFunSuite

class ExceptionSuite extends AnyFunSuite {
  test("cyclic") {
    var e1Cause: Throwable = null
    val e1 = new Exception {
      override def getCause: Throwable = e1Cause
    }
    val e2 = new Exception(e1)
    e1Cause = e2
    MdocExceptions.trimStacktrace(e1)
  }
}
