package mdoc.document

final case class InstrumentedInput(filename: String, text: String)
object InstrumentedInput {
  val empty = InstrumentedInput("", "")
}

final case class Document(instrumented: InstrumentedInput, sections: List[Section])

object Document {
  val empty: Document = Document(InstrumentedInput.empty, Nil)
  def empty(input: InstrumentedInput): Document = Document(input, Nil)
}

final case class Section(statements: List[Statement])

final case class Statement(
    binders: List[Binder[_]],
    out: String,
    position: RangePosition
)
