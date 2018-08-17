# Vork: markdown worksheets

[![Build Status](https://travis-ci.org/olafurpg/vork.svg?branch=master)](https://travis-ci.org/olafurpg/vork)

Vork is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, Vork interprets Scala code
examples in markdown files allowing you to compile markdown documentation as
part of your build. Distinguishing features of Vork include:

- [good performance](#performance): a tight edit/render/preview feedback loop
  makes it fun to write documentation.
- [script semantics](#script-semantics): markdown documents are compiled into
  regular Scala programs making code examples copy-paste friendly.
- [good error messages](#good-error-messages): compile errors and crashes are
  reported with positions of the original markdown source.
- [link hygiene](#link-hygiene): compile errors and crashes are reported with
  positions of the original markdown source.
- variable injection: instead of hardcoding constants, use variables like
  `@@VERSION@` to make sure the documentation remains up-to-date.
- extensible: library APIs expose hooks to customize rendering of code fences.

Table of contents:

<!-- TOC depthFrom:2 -->

- [Quickstart](#quickstart)
  - [Library](#library)
  - [Command-line](#command-line)
- [Modifiers](#modifiers)
  - [Default](#default)
  - [Fail](#fail)
  - [Crash](#crash)
  - [Passthrough](#passthrough)
- [Key features](#key-features)
  - [Performance](#performance)
  - [Good error messages](#good-error-messages)
  - [Link hygiene](#link-hygiene)
  - [Script semantics](#script-semantics)
- [Team](#team)
- [--help](#--help)

<!-- /TOC -->

## Quickstart

Starting from an empty directory, let's create a `docs` directory and
`docs/readme.md` file with some basic documentation

````
$ tree
.
└── docs
    └── readme.md
$ cat docs/readme.md
# My Project

To install my project
```scala
libraryDependencies += "com" % "lib" % "@@MY_VERSION@"
```

```scala vork
val x = 1
List(x, x)
```
````

Then we generate the site using either the [command-line](#command-line)
interface or [library API](#library). The resulting readme will look like this

````
# My Project

To install my project

```scala
libraryDependencies += "com" % "lib" % "1.0.0"
```

```scala
val x = 1
// x: Int = 1
List(x, x)
// res0: List[Int] = List(1, 1)
```
````

Observe that `MY_VERSION` has been replaced with `1.0.0` and that the
`scala vork` code fence has been interpreted by the Scala compiler.

### Library

Add the following dependency to your build

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/vork_2.12.6/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/vork_2.12.6)
```scala
// build.sbt
libraryDependencies += "com.geirsson" % "vork" % "@VERSION@" cross CrossVersion.full
```

Then write a main function that invokes Vork as a library

```scala
object Website {
  def main(args: Array[String]): Unit = {
    // build arguments for Vork
    val settings = vork.MainSettings()
      .withSiteVariables(Map("MY_VERSION" -> "1.0.0"))
    // generate out/readme.md from working directory
    val exitCode = vork.Main.process(settings)
    // (optional) exit the main function with exit code 0 (success) or 1 (error)
    sys.exit(exitCode)
  }
}
```

Consult the Vork
[documentation](https://www.javadoc.io/doc/com.geirsson/vork_2.12.6/@VERSION@) to
learn more how to use the library API. Note that code in the package
`vork.internal` is subject to binary and source breaking changes between any
release, including PATCH versions.

### Command-line

First, install the
[coursier command-line interface](https://github.com/coursier/coursier/#command-line).
Then run the following command:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/vork_2.12.6/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/vork_2.12.6)
```
$ coursier launch com.geirsson:vork_2.12.6:@VERSION@ -- --site.MY_VERSION 1.0.0
info: Compiling docs/readme.md
info:   done => out/readme.md (120 ms)
```

It's possible to customize the input and output directory with `--in` and
`--out`. There is also a `--watch` flag to watch for file changes and
incrementally re-generate pages on file save providing fast feedback while
Consult [`--help`](#--help) to learn more about the command-line interface.

## Modifiers

Vorks supports several modifiers to control the output of code fences.

### Default

The default modifier compiles and executes the code fence as normal

````scala vork:vork
```scala vork
val x = 1
val y = 2
x + y
```
````

### Fail

The `fail` modifier asserts that the code block will not compile

````scala vork:vork
```scala vork:fail
val x: Int = ""
```
````

Note that `fail` does not assert that the program compiles but crashes at
runtime. To assert runtime exceptions, use the `crash` modifier.

### Crash

The `crash` modifier asserts that the code block throws an exception at runtime

````scala vork:vork
```scala vork:crash
val y = ???
```
````

### Passthrough

The `passthrough` modifier collects the stdout and stderr output from the
program and embeds it verbatim in the markdown file.

````scala vork:vork
```scala vork:passthrough
val matrix = Array.tabulate(4, 4) { (a, b) =>
  val multiplied = (a + 1) * (b + 1)
  f"$multiplied%2s"
}
val table = matrix.map(_.mkString("| ", " | ", " |")).mkString("\n")
println(s"""
This will be rendered as markdown.

* Bullet 1
* Bullet 2

Look at the table:

$table
""")
```
````

## Key features

### Performance

Vork is designed to provide a tight edit/render/preview feedback loop while
writing documentation. Vork achieves good performance through

- [script semantics](#script-semantics): each markdown file compiles into a
  single Scala program that executes in one run.
- being incremental: with `--watch`, Vork compiles individual files as they
  change avoiding unnecessary work re-generating the full site.
- keeping the compiler hot: with `--watch`, Vork re-uses the same Scala compiler
  instance for subsequent runs making compilation faster after a few iterations.
  A medium sized document can go from compiling in ~5 seconds with a cold
  compiler down to 500ms with a hot compiler.

### Good error messages

Vork tries to report helpful error messages when things go wrong. Here below,
the program that is supposed to compile successfully but it has a type error so
the build is stopped with an error message from the Scala compiler.

````scala vork:vork:crash
```scala vork
val typeError: Int = "should be int"
```
````

Here below, the programs are supposed to fail due to the `fail` and `crash`
modifiers but they succeed so the build is stopped with an error message from
Vork.

````scala vork:vork:crash
```scala vork:fail
val noFail = "success"
```
```scala vork:crash
val noCrash = "success"
```
````

Observe that positions of the reported diagnostics point to line numbers and
columns in the original markdown document. Internally, Vork instruments code
fences to extract metadata like variable types and runtime values. Positions of
error messages in the instrumented code are translated into positions in the
markdown document.

### Link hygiene

Docs get quickly out date, in particular links to different sections. After
generating a site, Vork analyzes links for references to non-existent sections.
For the example below, Vork reports a warning that the `doesnotexist` link is
invalid.

```scala vork:vork:crash
# My title

Link to [my title](#my-title).
Link to [old section](#doesnotexist).
```

### Script semantics

Vork interprets code fences as normal Scala programs instead of as if they're
evaluated in the REPL. This behavior is different from tut that interprets
statements as if they were typed in a REPL session. Using "script semantics"
instead of "repl semantics" has both benefits and downsides.

**Downside**: It's not possible to bind the same variable twice, for example the
code below input fails compilation with Vork but compiles successfully with tut

````
```scala vork
val x = 1
val x = 1
```
````

**Upside**: Code examples can be copy-pasted into normal Scala programs and
compile.

**Upside**: Companion objects Just Work™️

````scala vork:vork
```scala vork
case class User(name: String)
object User {
  implicit val ordering: Ordering[User] = Ordering.by(_.name)
}
List(User("John"), User("Susan")).sorted
```
````

## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!

## --help

````scala vork:passthrough
println("```")
println(vork.internal.cli.Settings.help.helpMessage(80))
println("```")
````
