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

  checkCli(
    "nowarn override --check-link-hygiene",
    original,
    original,
    extraArgs = Array(
      "--no-link-hygiene",
      "--check-link-hygiene"
    ),
    onStdout = { out =>
      assert(
        out.contains(
          "warning: --no-link-hygiene and --check-link-hygiene are mutually exclusive. Link hygiene is disabled."
        )
      )
    }
  )

}
