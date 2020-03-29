package tests.markdown

import tests.cli.BaseCliSuite

class NoLinkWarningSuite extends BaseCliSuite {
  val original =
    """/readme.md
      |# Header
      |
      |[Head](#head)
    """.stripMargin

  checkCli(
    "nowarn",
    original,
    original,
    extraArgs = Array(
      "--no-link-hygiene"
    ),
    onStdout = { out => assert(!out.contains("warning")) }
  )

}
