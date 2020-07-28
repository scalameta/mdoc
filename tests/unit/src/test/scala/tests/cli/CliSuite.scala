package tests.cli

import java.nio.file.Files
import mdoc.internal.BuildInfo
import scala.meta.io.AbsolutePath
import java.nio.file.Paths

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
      tests.cli.BuildInfo.testsInputClassDirectory.getAbsolutePath
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
    setup = { () =>
      // This file should be overwritten
      Files.write(out().resolve("licence.txt").toNIO, "Apache".getBytes())
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
    onStdout = { out => assert(out.contains("mdoc is a documentation tool")) }
  )

  checkCli(
    "usage",
    "",
    "",
    extraArgs = Array("--usage"),
    onStdout = { out => assert(out.contains("mdoc [<option> ...]")) }
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

  checkCli(
    "single-in",
    """
      |/index.md
      |# Single file
      |```scala mdoc
      |println("one file")
      |```
      |/second.md
      |```scala mdoc
      |println("second file")
      |```
      |""".stripMargin,
    """|/index.md
       |# Single file
       |```scala
       |println("one file")
       |// one file
       |```
       |""".stripMargin,
    input = { in().resolve("index.md").toString }
  )

  checkCli(
    "single-in-single-out",
    """
      |/index.md
      |# Single file
      |```scala mdoc
      |println("one file")
      |```
      |""".stripMargin,
    """
      |/out.md
      |# Single file
      |```scala
      |println("one file")
      |// one file
      |```
      |""".stripMargin,
    input = { in().resolve("index.md").toString },
    output = { out().resolve("out.md").toString }
  )

  checkCli(
    "single-in-single-out-only",
    """
      |/index.md
      |# Single file
      |```scala mdoc
      |println("one file")
      |```
      |/second.md
      |```scala mdoc
      |println("second file")
      |```
      |""".stripMargin,
    """
      |/out.md
      |# Single file
      |```scala
      |println("one file")
      |// one file
      |```
      |""".stripMargin,
    input = { in().resolve("index.md").toString },
    output = { out().resolve("out.md").toString }
  )

  checkCli(
    "multiple-in",
    """
      |/index1.md
      |```scala mdoc
      |println("1 file")
      |```
      |/index2.md
      |```scala mdoc
      |println("2 file")
      |```
      |/index3.md
      |```scala mdoc
      |println("3 file")
      |```
      |""".stripMargin,
    """
      |/out1.md
      |```scala
      |println("1 file")
      |// 1 file
      |```
      |/out2.md
      |```scala
      |println("2 file")
      |// 2 file
      |```
      |""".stripMargin,
    input = "index1.md",
    output = out().resolve("out1.md").toString,
    extraArgs = Array("--in", "index2.md", "--out", out().resolve("out2.md").toString)
  )

  checkCli(
    "multiple-out-directories",
    """
      |/in1/index.md
      |```scala mdoc
      |println("1 file")
      |```
      |/in2/index.md
      |```scala mdoc
      |println("2 file")
      |```
      |/in3/index.md
      |```scala mdoc
      |println("3 file")
      |```
      |""".stripMargin,
    """
      |/out1/index.md
      |```scala
      |println("1 file")
      |// 1 file
      |```
      |/out2/index.md
      |```scala
      |println("2 file")
      |// 2 file
      |```
      |""".stripMargin,
    input = "in1",
    output = out().resolve("out1").toString,
    extraArgs = Array("--in", "in2", "--out", out().resolve("out2").toString)
  )

  checkCli(
    "conflicting-out",
    """
      |/in1/index.md
      |```scala mdoc
      |println("1 file")
      |```
      |/in2/index.md
      |```scala mdoc
      |println("2 file")
      |```
      |""".stripMargin,
    // NOTE(olafur) Last one wins in case of conflict. Feel free to update the
    // expected behavior here if we want to error instead. This test is only to
    // document that the current behavior even if this behavior is undesirable.
    """
      |/out/index.md
      |```scala
      |println("2 file")
      |// 2 file
      |```
      |""".stripMargin,
    input = "in1",
    output = out().resolve("out").toString,
    extraArgs = Array("--in", "in2", "--out", out().resolve("out").toString)
  )

}
