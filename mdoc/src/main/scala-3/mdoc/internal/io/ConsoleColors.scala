package mdoc.internal.io

case class ConsoleColors(
    green: String => String = identity,
    blue:  String => String = identity,
    yellow:  String => String = identity,
    red:  String => String = identity
)

