package vork

import vork.internal.BuildInfo

class CliSuite extends BaseCliSuite {

  checkCli(
    "vork.conf",
    """
      |/index.md
      |# Hello ![version]
      |/vork.conf
      |site.version = "1.0"
    """.stripMargin,
    """
      |/index.md
      |# Hello 1.0
      |
      |
      |/vork.conf
      |site.version = "1.0"
    """.stripMargin
  )

  checkCli(
    "classpath",
    """
      |/index.md
      |```scala vork
      |test.Test.greeting
      |```
    """.stripMargin,
    """
      |/index.md
      |```scala
      |@ test.Test.greeting
      |res0: String = "Hello world!"
      |```
    """.stripMargin,
    extraArgs = Array(
      "--classpath",
      BuildInfo.testsInputClassDirectory.getAbsolutePath
    )
  )

  checkCli(
    "include/exclude",
    """
      |/include.md
      |# Include
      |/index.md
      |# Index
      |/include-exclude.md
      |# Exclude
    """.stripMargin,
    """
      |/include.md
      |# Include
    """.stripMargin,
    extraArgs = Array(
      "--include-files",
      "include",
      "--exclude-files",
      "exclude"
    )
  )

  checkCli(
    "non-markdown",
    """
      |/licence.txt
      |MIT
    """.stripMargin,
    """
      |/licence.txt
      |MIT
    """.stripMargin
  )

}
