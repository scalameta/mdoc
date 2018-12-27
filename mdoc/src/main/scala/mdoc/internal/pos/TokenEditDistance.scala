package mdoc.internal.pos

import difflib._
import difflib.myers.Equalizer
import scala.annotation.tailrec
import scala.meta._
import mdoc.internal.pos.PositionSyntax._

/** Helper to map between position between two similar strings. */
final class TokenEditDistance private (matching: Array[MatchingToken]) {
  private def isEmpty: Boolean = matching.length == 0
  def originalInput: Input =
    if (isEmpty) Input.None
    else matching(0).original.input

  def revisedInput: Input =
    if (isEmpty) Input.None
    else matching(0).revised.input

  def toRevised(
      originalLine: Int,
      originalColumn: Int
  ): Either[EmptyResult, Position] = {
    toRevised(originalInput.toOffset(originalLine, originalColumn).start)
  }

  /** Convert from offset in original string to offset in revised string */
  def toRevised(originalOffset: Int): Either[EmptyResult, Position] = {
    if (isEmpty) EmptyResult.unchanged
    else {
      BinarySearch
        .array[MatchingToken](
          matching,
          mt => compare(mt.original.pos, originalOffset)
        )
        .fold(EmptyResult.noMatch)(m => Right(m.revised.pos))
    }
  }

  def toOriginal(
      revisedLine: Int,
      revisedColumn: Int
  ): Either[EmptyResult, Position] = {
    toOriginal(revisedInput.toOffset(revisedLine, revisedColumn).start)
  }

  /** Convert from offset in revised string to offset in original string */
  def toOriginal(revisedOffset: Int): Either[EmptyResult, Position] = {
    if (isEmpty) EmptyResult.unchanged
    else {
      BinarySearch
        .array[MatchingToken](
          matching,
          mt => compare(mt.revised.pos, revisedOffset)
        )
        .fold(EmptyResult.noMatch)(m => Right(m.original.pos))
    }
  }

  private def compare(
      position: Position,
      offset: Int
  ): BinarySearch.ComparisonResult = {
    val pos = position.toUnslicedPosition
    if (pos.contains(offset)) BinarySearch.Equal
    else if (pos.end <= offset) BinarySearch.Smaller
    else BinarySearch.Greater
  }

}

object TokenEditDistance {

  lazy val empty: TokenEditDistance = new TokenEditDistance(Array.empty)

  /**
    * Build utility to map offsets between two slightly different strings.
    *
    * @param original The original snapshot of a string, for example the latest
    *                 semanticdb snapshot.
    * @param revised The current snapshot of a string, for example open buffer
    *                in an editor.
    */
  def apply(original: IndexedSeq[Token], revised: IndexedSeq[Token]): TokenEditDistance = {
    val buffer = Array.newBuilder[MatchingToken]
    buffer.sizeHint(math.max(original.length, revised.length))
    @tailrec
    def loop(
        i: Int,
        j: Int,
        ds: List[Delta[Token]]
    ): Unit = {
      val isDone: Boolean =
        i >= original.length ||
          j >= revised.length
      if (isDone) ()
      else {
        val o = original(i)
        val r = revised(j)
        if (TokenEqualizer.equals(o, r)) {
          buffer += MatchingToken(o, r)
          loop(i + 1, j + 1, ds)
        } else {
          ds match {
            case Nil =>
              loop(i + 1, j + 1, ds)
            case delta :: tail =>
              loop(
                i + delta.getOriginal.size(),
                j + delta.getRevised.size(),
                tail
              )
          }
        }
      }
    }
    val deltas = {
      import scala.collection.JavaConverters._
      difflib.DiffUtils
        .diff(original.asJava, revised.asJava, TokenEqualizer)
        .getDeltas
        .iterator()
        .asScala
        .toList
    }
    loop(0, 0, deltas)
    new TokenEditDistance(buffer.result())
  }

  def apply(
      originalInput: Input,
      revisedInput: Input
  ): Option[TokenEditDistance] = {
    for {
      revised <- revisedInput.tokenize.toOption
      original <- {
        if (originalInput == revisedInput) Some(revised)
        else originalInput.tokenize.toOption
      }
    } yield apply(original, revised)
  }

  /** Compare tokens only by their text and token category. */
  private object TokenEqualizer extends Equalizer[Token] {
    override def equals(original: Token, revised: Token): Boolean =
      original.productPrefix == revised.productPrefix &&
        original.pos.text == revised.pos.text
  }

  def toTokenEdit(original: Seq[Tree], instrumented: Input): TokenEditDistance = {
    val instrumentedTokens = instrumented.tokenize.get
    val originalTokens: Array[Token] = {
      val buf = Array.newBuilder[Token]
      original.foreach { tree =>
        tree.tokens.foreach { token =>
          buf += token
        }
      }
      buf.result()
    }
    TokenEditDistance(originalTokens, instrumentedTokens)
  }

}
