package tests.cli

import java.nio.file.Files
import vork.internal.BuildInfo

class CliSuite extends BaseCliSuite {

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
      "--include-path",
      "include.md",
      "--exclude-path",
      "exclude.md"
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
      "--exclude-path",
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
      |```scala vork:foobar
      |foobar
      |```
    """.stripMargin,
    "", // empty site, did not generate
    expectedExitCode = 1,
    onStdout = { out =>
      assert(out.contains("Invalid mode 'foobar'"))
    }
  )

}
