package tests.cli

import java.nio.file.Files

import mdoc.internal.BuildInfo
import tests.markdown.{LifeCycleCounter, LifeCycleModifier}

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
      |[Head](#head)
    """.stripMargin,
    """
      |/before-index.md
      |# Header
      |[Head](#head)
    """.stripMargin, // did not generate index.md
    expectedExitCode = 1,
    onStdout = { out =>
      assert(out.contains("Compiling 2 files to"))
      assert(out.contains("Invalid mode 'foobar'"))
      assert(out.contains("Compiled in"))
      assert(out.contains("(1 error, 1 warning)"))
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

  checkCli(
    "html",
    """
      |/index.html
      |<h1>My Fantastic Talk!</h1>
      |<p>
      |```scala mdoc
      |println(42)
      |```
      |</p>
      |""".stripMargin,
    """
      |/index.html
      |<h1>My Fantastic Talk!</h1>
      |<p>
      |```scala
      |println(42)
      |// 42
      |```
      |</p>
      |""".stripMargin
  )

  checkCliMulti(
    "lifeCycle-0",
    """
      |/file1.md
      |# file 1
      |One
      |```scala mdoc:lifecycle
      |val x1 = 1
      |```
      |/file2.md
      |# file 2
      |Two
      |```scala mdoc:lifecycle
      |val x2 = 2
      |```
      |    """.stripMargin,
    List(
      """
        |/file1.md
        |# file 1
        |One
        |numberOfStarts = 1 ; numberOfExists = 0
        |/file2.md
        |# file 2
        |Two
        |numberOfStarts = 1 ; numberOfExists = 0
    """.stripMargin,
      """
        |/file1.md
        |# file 1
        |One
        |numberOfStarts = 1 ; numberOfExists = 0
        |/file2.md
        |# file 2
        |Two
        |numberOfStarts = 1 ; numberOfExists = 0
    """.stripMargin
    ), // process counts per PostModifier instance, starts and exists per mdoc.Main process
    setup = { fixture =>
      // Global thread local counter updated by all mdoc.Main process
      // All tests in this test suite run sequentially but change the counter
      // So make sure we start anew for this test
      LifeCycleCounter.numberOfStarts.set(0)
      LifeCycleCounter.numberOfExists.set(0)
    },
    onStdout = { out =>
      assert(out.contains("Compiling 2 files to"))
      assert(out.contains("Compiled in"))
      assert(out.contains("(0 errors"))
      // Should start and stop one only once in this test (several times for test-suite)
      assert(LifeCycleCounter.numberOfExists.get() == 1)
      assert(LifeCycleCounter.numberOfStarts.get() == 1)
    }
  )

}
