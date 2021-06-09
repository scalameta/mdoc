package tests.imports

import tests.markdown.BaseMarkdownSuite
import tests.cli.BaseCliSuite
import scala.meta.io.RelativePath
import java.io.File

class FileSuite extends BaseCliSuite {

  val includeOutputPath: RelativePath => Boolean = { path => path.toNIO.endsWith("readme.md") }
  checkCli(
    "basic".tag(SkipScala3),
    """
      |/hello.sc
      |val message = "Hello world!"
      |/readme.md
      |```scala mdoc
      |import $file.hello
      |println(hello.message)
      |```
      |""".stripMargin,
    """
      |/readme.md
      |```scala
      |import $file.hello
      |println(hello.message)
      |// Hello world!
      |```
      |""".stripMargin,
    includeOutputPath = includeOutputPath
  )

  checkCli(
    "outer".tag(SkipScala3),
    """
      |/inner/hello.sc
      |val message = "hello world"
      |/readme.md
      |```scala mdoc
      |import $file.inner.hello
      |println(hello.message)
      |```
      |""".stripMargin,
    """
      |/readme.md
      |```scala
      |import $file.inner.hello
      |println(hello.message)
      |// hello world
      |```
      |""".stripMargin,
    includeOutputPath = includeOutputPath
  )

  1.to(3).foreach { i =>
    val caret = "^" * i
    val inner = 1.to(i).map(_ => "inner").mkString("/")
    checkCli(
      inner.tag(SkipScala3),
      s"""
         |/hello.sc
         |val message = "hello world"
         |/$inner/readme.md
         |```scala mdoc
         |import $$file.$caret.hello
         |println(hello.message)
         |```
         |""".stripMargin,
      s"""|/$inner/readme.md
          |```scala
          |import $$file.$caret.hello
          |println(hello.message)
          |// hello world
          |```
          |""".stripMargin,
      includeOutputPath = includeOutputPath
    )
  }

  checkCli(
    "nested".tag(SkipScala3),
    """
      |/hello1.sc
      |val first = "hello"
      |val second = "world"
      |/hello2.sc
      |import $file.hello1
      |val first = hello1.first
      |/hello3.sc
      |import $file.hello1
      |val second = hello1.second
      |/readme.md
      |```scala mdoc
      |import $file.hello2, $file.hello3
      |println(s"${hello2.first} ${hello3.second}")
      |```
      |""".stripMargin,
    """|/readme.md
       |```scala
       |import $file.hello2, $file.hello3
       |println(s"${hello2.first} ${hello3.second}")
       |// hello world
       |```
       |""".stripMargin,
    includeOutputPath = includeOutputPath
  )

  checkCli(
    "cycles",
    """
      |/hello1.sc
      |import $file.hello2
      |val first = hello2.first
      |/hello2.sc
      |import $file.hello1
      |val first = hello1.first
      |/readme.md
      |```scala mdoc
      |import $file.hello1
      |println(s"${hello1.first} world")
      |```
      |""".stripMargin,
    "",
    expectedExitCode = 1,
    includeOutputPath = includeOutputPath,
    onStdout = { stdout =>
      assertNoDiff(
        stdout,
        """|info: Compiling 3 files to <output>
           |error: readme.md:2:14: illegal cyclic dependency. To fix this problem, refactor the code so that no transitive $file imports end up depending on the original file.
           | -- root       --> readme.md:1
           | -- depends on --> hello1.sc:0
           | -- depends on --> hello2.sc:0
           | -- cycle      --> <input>/hello1.sc
           |import $file.hello1
           |             ^^^^^^
           |""".stripMargin
      )
    }
  )

  checkCli(
    "compile-error".tag(SkipScala3),
    """
      |/hello1.sc
      |val message: String = 42
      |/hello2.sc
      |import $file.hello1
      |val number: Int = ""
      |/readme.md
      |```scala mdoc
      |import $file.hello2
      |val something: Int = ""
      |println(hello2.number)
      |```
      |""".stripMargin,
    "",
    expectedExitCode = 1,
    includeOutputPath = includeOutputPath,
    onStdout = { stdout =>
      assertNoDiff(
        stdout,
        """|info: Compiling 3 files to <output>
           |error: <input>/hello1.sc:1:23: type mismatch;
           | found   : Int(42)
           | required: String
           |val message: String = 42
           |                      ^^
           |error: <input>/hello2.sc:2:19: type mismatch;
           | found   : String("")
           | required: Int
           |val number: Int = ""
           |                  ^^
           |error: readme.md:3:22: type mismatch;
           | found   : String("")
           | required: Int
           |val something: Int = ""
           |                     ^^
           |""".stripMargin
      )
    }
  )

  checkCli(
    "conflicting-package".tag(SkipScala3),
    """
      |/hello0.sc
      |val zero = 0
      |/inner/hello1.sc
      |val one = 1
      |/inner/hello2.sc
      |import $file.hello1
      |import $file.^.hello0
      |val two = hello1.one + 1 + hello0.zero
      |/hello3.sc
      |import $file.hello0
      |import $file.inner.hello1
      |import $file.inner.hello2
      |val three = hello0.zero + hello1.one + hello2.two
      |/readme.md
      |```scala mdoc
      |import $file.hello3
      |println(hello3.three)
      |```
      |""".stripMargin,
    """|/readme.md
       |```scala
       |import $file.hello3
       |println(hello3.three)
       |// 3
       |```
       |""".stripMargin,
    includeOutputPath = includeOutputPath
  )

  checkCli(
    "importees".tag(SkipScala3),
    """
      |/hello0.sc
      |val zero = 0
      |/hello1.sc
      |val one = 1
      |/inner/hello2.sc
      |import $file.^.{ hello1 => h1 }
      |val two = h1.one + 1
      |/readme.md
      |```scala mdoc
      |import $file.{ hello0, hello1 => h1 }, $file.inner.hello2
      |println(hello0.zero)
      |println(h1.one)
      |println(hello2.two)
      |```
      |""".stripMargin,
    """|/readme.md
       |```scala
       |import $file.{ hello0, hello1 => h1 }, $file.inner.hello2
       |println(hello0.zero)
       |// 0
       |println(h1.one)
       |// 1
       |println(hello2.two)
       |// 2
       |```
       |""".stripMargin,
    includeOutputPath = includeOutputPath
  )

  checkCli(
    "importee-unimport",
    """
      |/hello0.sc
      |val zero = 0
      |/readme.md
      |```scala mdoc
      |import $file.{ hello0 => _ }
      |println("Hello world!")
      |```
      |""".stripMargin,
    "",
    expectedExitCode = 1,
    includeOutputPath = includeOutputPath,
    onStdout = { stdout =>
      assertNoDiff(
        stdout,
        """|info: Compiling 2 files to <output>
           |error: readme.md:2:16: unimports are not supported for $file imports. To fix this problem, remove the unimported symbol.
           |import $file.{ hello0 => _ }
           |               ^^^^^^^^^^^
           |""".stripMargin
      )
    }
  )

  checkCli(
    "importee-wildcard",
    """
      |/hello0.sc
      |val zero = 0
      |/readme.md
      |```scala mdoc
      |import $file._
      |println(hello0.zero)
      |```
      |""".stripMargin,
    "",
    expectedExitCode = 1,
    includeOutputPath = includeOutputPath,
    onStdout = { stdout =>
      assertNoDiff(
        stdout,
        """|info: Compiling 2 files to <output>
           |error: readme.md:2:14: wildcards are not supported for $file imports. To fix this problem, explicitly import files using the `import $file.FILENAME` syntax.
           |import $file._
           |             ^
           |""".stripMargin
      )
    }
  )

  checkCli(
    "rename-edit-distance".tag(SkipScala3),
    """
      |/hello0.sc
      |val zero = 0
      |/inner/hello1.sc
      |import $file.^.hello0
      |val one = hello0.number + 1
      |/readme.md
      |```scala mdoc
      |import $file.hello0, $file.inner.hello1
      |println(hello0.zero)
      |println(hello1.one)
      |```
      |""".stripMargin,
    "",
    expectedExitCode = 1,
    includeOutputPath = includeOutputPath,
    onStdout = { stdout =>
      // NOTE(olafur): this test stresses that the caret position of the error
      // message points to `hello0.number` despite using package renames in the
      // import qualifier. This works because we employ token edit distance to
      // map positions from the instrumented source to the original source.
      assertNoDiff(
        stdout,
        """|info: Compiling 3 files to <output>
           |error: <input>/inner/hello1.sc:2:11: value number is not a member of object $file.hello0
           |val one = hello0.number + 1
           |          ^^^^^^^^^^^^^
           |""".stripMargin
      )
    }
  )

}
