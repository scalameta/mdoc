package mdoc.internal.document

import Compat.TPrint

object Printing {
  inline def stringValue[T](value: T) = nullableToString(value)
  inline def typeString[T](tprint: TPrint[T]) = tprint.render

  inline def print[T](value: T, out: StringBuilder, width: Int, height: Int) = {
    out.append(nullableToString(value))
  }

  inline def printOneLine[T](value: T, out: StringBuilder, width: Int) = {
    out.append(nullableToString(value).replace("\n", "").replaceAll("\\s+", " "))
  }

  private def nullableToString[T](value: T): String = {
    value match
      case arr: Array[?] => arr.map(nullableToString).mkString("Array(", ", ", ")")
      case null => "null"
      case _ => value.toString()
  }
}
