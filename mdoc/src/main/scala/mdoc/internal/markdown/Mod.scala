package mdoc.internal.markdown

import scala.util.Try

sealed abstract class Mod extends Product with Serializable
object Mod {
  case object Fail extends Mod
  case object Warn extends Mod
  case object Crash extends Mod
  case object Silent extends Mod
  case object Passthrough extends Mod
  case object Invisible extends Mod
  case object CompileOnly extends Mod {
    override def toString: String = "compile-only"
  }
  case object Reset extends Mod
  case object ResetClass extends Mod {
    override def toString: String = "reset-class"
  }
  case object ResetObject extends Mod {
    override def toString: String = "reset-object"
  }
  case object ToString extends Mod {
    override def toString: String = "to-string"
  }
  case object Nest extends Mod

  case class Width(value: Int) extends Mod {
    override def toString: String = s"width=$value"
  }

  case class Height(value: Int) extends Mod {
    override def toString: String = s"height=$value"
  }

  def static: List[Mod] =
    List(
      Passthrough,
      Invisible,
      CompileOnly,
      Reset,
      ResetClass,
      ResetObject,
      Fail,
      Warn,
      Crash,
      Silent,
      ToString,
      Nest
    )

  val parametric: PartialFunction[String, Mod] = {
    val ToWidth = "width=(\\d+)".r
    val ToHeight = "height=(\\d+)".r
    object ToInt {
      def unapply(s: String): Option[Int] = Try(s.toInt).toOption
    }

    {
      case ToWidth(ToInt(n)) => Width(n)
      case ToHeight(ToInt(n)) => Height(n)
    }
  }

  def unapply(string: String): Option[Mod] = {
    static.find(_.toString.equalsIgnoreCase(string)) orElse parametric.lift(string)
  }
}
