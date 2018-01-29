package vork.markdown.repl

import fastparse.core.Parsed
import vork.markdown.processors.FencedCodeMod

object Evaluator {
  case class CodeFenceFailure(errors: List[String])
      extends Exception("Vork found evaluation failures.\n" + errors.mkString("\n", "\n\n", "\n"))

  import ammonite.interp.Parsers
  def parseCode(code: String): Seq[String] = {
    Parsers.Splitter.parse(code) match {
      case Parsed.Success(value, idx) => value
      case Parsed.Failure(_, index, extra) =>
        sys.error(
          fastparse.core.ParseError.msg(extra.input, extra.traced.expected, index)
        )
    }
  }

  def evaluate(
      repl: AmmoniteRepl,
      block: RichCodeBlock,
      mod: FencedCodeMod
  ): Either[String, String] = {
    val out = new StringBuilder()
    val initial: Either[String, StringBuilder] = Right(out)
    val res = parseCode(block.code).toList.foldLeft(initial) {
      case (acc, stmt) =>
        acc match {
          case l: Left[String, StringBuilder] => l
          case r: Right[String, StringBuilder] =>
            val result = repl.run(stmt)
            (result.evaluated.isSuccess, mod) match {
              case (false, FencedCodeMod.Fail) =>
                Right(out.append("@ ").append(stmt).append(result.error).append("\n"))
              case (true, FencedCodeMod.Passthrough) =>
                Right(out.append(result.stdout))
              case (true, FencedCodeMod.Default) =>
                Right(out.append("@ ").append(stmt).append(result.outString).append("\n"))
              case (true, FencedCodeMod.Fail) =>
                Left(s"${block.pos}: unexpected success of\n```\n${block.code}```")
              case (false, _) =>
                val receivedError = result.error.split("\n").map(l => s">  $l").mkString("\n")
                Left(s"${block.pos}: unexpected failure\n$receivedError")
            }
        }
    }

    res.map { out =>
      if (out.size > 0 && out.charAt(out.size - 1) == '\n')
        out.setLength(out.length - 1)
      out.toString()
    }
  }
}
