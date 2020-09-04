package mdoc.internal.document

import Compat.TPrint

object Printing {
    inline def stringValue[T](value: T) = value.toString
    inline def typeString[T](tprint: TPrint[T]) = tprint.render
 
    inline def print[T](value: T, out : StringBuilder, width: Int, height: Int) = {
       out.append(value.toString)
    }

    inline def printOneLine[T](value: T, out : StringBuilder, width: Int) = {
       out.append(value.toString.replace("\n",""))
    }
}
