package fox.markdown.repl

import fastparse.core.Parsed
import fox.markdown.processors.FencedCodeMod

object Evaluator {
  case class CodeFenceError(msg: String) extends Exception(msg)
  case class CodeFenceSuccess(msg: String) extends Exception(msg)

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

  def evaluate(repl: AmmoniteRepl, code: String, mod: FencedCodeMod): String = {
    val out = new StringBuilder()
    parseCode(code).foreach { stmt =>
      val result = repl.run(stmt, repl.currentLine)
      (result.evaluated.isSuccess, mod) match {
        case (false, FencedCodeMod.Fail) =>
          out.append("@ ").append(stmt).append(result.error).append("\n")
        case (true, FencedCodeMod.Passthrough) => out.append(result.stdout)
        case (true, FencedCodeMod.Default) =>
          out.append("@ ").append(stmt).append(result.outString).append("\n")
        case (true, FencedCodeMod.Fail) =>
          throw CodeFenceSuccess(s"expected failure, got success for code block:\n$code")
        case (false, _) =>
          throw CodeFenceError(
            s"expected success, got failure ${pprint.apply(result)} for code block:\n$code"
          )
      }

    }

    if (out.charAt(out.size - 1) == '\n') { out.setLength(out.length - 1) } // strip trailing newline
    out.toString()
  }
}
