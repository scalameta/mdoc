package tests.cli

import java.nio.file.Files
import mdoc.internal.BuildInfo

class CliSuite extends BaseCliSuite {

  checkCli(
    "formatting",
    """
      |/index.md
      |```scala mdoc
      |List(1,
      |  2, 3, 4) // comment
      |```
    """.stripMargin,
    """
      |/index.md
      |```scala
      |List(1,
      |  2, 3, 4) // comment
      |// res0: List[Int] = List(1, 2, 3, 4)
      |```
    """.stripMargin
  )

  checkCli(
    "classpath",
    """
      |/index.md
      |```scala mdoc
      |test.Test.greeting
      |```
    """.stripMargin,
    """
      |/index.md
      |```scala
      |test.Test.greeting
      |// res0: String = "Hello world!"
      |```
    """.stripMargin,
    extraArgs = Array(
      "--classpath",
      tests.BuildInfo.testsInputClassDirectory.getAbsolutePath
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
      "--include",
      "include.md",
      "--exclude",
      "exclude.md"
    )
  )
  checkCli(
    "include one file",
    """
      |/users/install.md
      |# Install
      |/users/usage.md
      |# Usage
      |/developers/setup.md
      |# Setup
    """.stripMargin,
    """
      |/users/install.md
      |# Install
    """.stripMargin,
    extraArgs = Array(
      "--include",
      "**/install.md"
    )
  )

  checkCli(
    "exclude-dir",
    """
      |/index.md
      |# Index @version@
      |/src/Foo.md
      |# Foo @version@
      |/src/Bar.md
      |# Bar @version@
    """.stripMargin,
    """
      |/index.md
      |# Index 1.0.0
    """.stripMargin,
    extraArgs = Array(
      "--exclude",
      "src"
    )
  )

  checkCli(
    "copy-overwrite",
    """
      |/licence.txt
      |MIT
    """.stripMargin,
    """
      |/licence.txt
      |MIT
    """.stripMargin,
    setup = { fixture =>
      // This file should be overwritten
      Files.write(fixture.out.resolve("licence.txt"), "Apache".getBytes())
    }
  )

  checkCli(
    "error",
    """
      |/index.md
      |# Hello
      |```scala mdoc:foobar
      |foobar
      |```
      |/before-index.md
      |# Header
    """.stripMargin,
    """
      |/before-index.md
      |# Header
    """.stripMargin, // did not generate index.md
    expectedExitCode = 1,
    onStdout = { out =>
      assert(out.contains("Invalid mode 'foobar'"))
    }
  )

  checkCli(
    "help",
    "",
    "",
    extraArgs = Array("--help"),
    onStdout = { out =>
      assert(out.contains("mdoc is a documentation tool"))
    }
  )

  checkCli(
    "usage",
    "",
    "",
    extraArgs = Array("--usage"),
    onStdout = { out =>
      assert(out.contains("mdoc [<option> ...]"))
    }
  )

}
