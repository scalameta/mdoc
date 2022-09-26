package tests.markdown

import tests.cli.BaseCliSuite

class CheckLinkHygieneSuite extends BaseCliSuite {
  val original =
    """/readme.md
      |# Header
      |
      |[Head](#head)
    """.stripMargin

  checkCli(
    "fails",
    original,
    original,
    extraArgs = Array(
      "--check-link-hygiene"
    ),
    onStdout = { out => assert(out.contains("error")) },
    expectedExitCode = 1
  )

  val valid =
    """/readme.md
      |# Valid
    """.stripMargin

  checkCli(
    "pass",
    valid,
    valid,
    extraArgs = Array(
      "--check-link-hygiene"
    ),
    expectedExitCode = 0
  )

}
