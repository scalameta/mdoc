package fox.markdown

import com.vladsch.flexmark.util.sequence.{BasedSequence, CharSubSequence}

object FoxHelpers {
  implicit def stringToCharSequence(string: String): BasedSequence = {
    CharSubSequence.of(string)
  }
}
