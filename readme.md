# Mdoc: markdown worksheets

[![Build Status](https://travis-ci.org/olafurpg/mdoc.svg?branch=master)](https://travis-ci.org/olafurpg/mdoc)

Mdoc is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, Mdoc interprets Scala code
examples in markdown files allowing you to compile markdown documentation as
part of your build. Distinguishing features of Mdoc include:

- [good performance](#performance): incremental, hot compilation gives you
  snappy feedback while writing documentation.
- [script semantics](#script-semantics): markdown documents are compiled into
  normal Scala programs making examples copy-paste friendly.
- [good error messages](#good-error-messages): compile errors and crashes are
  reported with positions of the original markdown source making it easy to
  track down where things went wrong.
- [link hygiene](#link-hygiene): catch broken links to non-existing sections
  while generating the site.
- variable injection: instead of hardcoding constants like versions numbers, use
  variables like `@VERSION@` to make sure the documentation stays up-to-date
  with new releases.
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
libraryDependencies += "com" % "lib" % "@MY_VERSION@"
```

```scala mdoc
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
`scala mdoc` code fence has been interpreted by the Scala compiler.

### Library

Add the following dependency to your build

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12.6/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12.6)

```scala
// build.sbt
libraryDependencies += "com.geirsson" % "mdoc" % "0.3.1" cross CrossVersion.full
```

Then write a main function that invokes Mdoc as a library

```scala
object Main {
  def main(args: Array[String]): Unit = {
    // build arguments for Mdoc
    val settings = mdoc.MainSettings()
      .withSiteVariables(Map("MY_VERSION" -> "1.0.0"))
      .withArgs(args.toList)
    // generate out/readme.md from working directory
    val exitCode = mdoc.Main.process(settings)
    // (optional) exit the main function with exit code 0 (success) or 1 (error)
    sys.exit(exitCode)
  }
}
```

Consult [--help](#--help) to see what arguments are valid for `withArgs`.

Consult the Mdoc source to learn more how to use the library API. Scaladocs are
available [here](https://www.javadoc.io/doc/com.geirsson/mdoc_2.12.6/0.3.1)
but beware there are limited docstrings for classes and methods. Keep in mind
that code in the package `mdoc.internal` is subject to binary and source
breaking changes between any release, including PATCH versions.

### Command-line

First, install the
[coursier command-line interface](https://github.com/coursier/coursier/#command-line).
Then run the following command:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12.6/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12.6)

```
$ coursier launch com.geirsson:mdoc_2.12.6:0.3.1 -- --site.MY_VERSION 1.0.0
info: Compiling docs/readme.md
info:   done => out/readme.md (120 ms)
```

It's possible to customize the input and output directory with `--in` and
`--out`. There is also a `--watch` flag to watch for file changes and
incrementally re-generate pages on file save providing fast feedback while
Consult [`--help`](#--help) to learn more about the command-line interface.

## Modifiers

Mdocs supports several modifiers to control the output of code fences.

### Default

The default modifier compiles and executes the code fence as normal


Before:

````
```scala mdoc
val x = 1
val y = 2
x + y
```
````

After:

````
```scala
val x = 1
// x: Int = 1

val y = 2
// y: Int = 2

x + y
// res0: Int = 3
```
````


### Fail

The `fail` modifier asserts that the code block will not compile


Before:

````
```scala mdoc:fail
val x: Int = ""
```
````

After:

````
```scala
val x: Int = ""
// type mismatch;
//  found   : String("")
//  required: Int
// val x: Int = ""
//              ^
```
````

Note that `fail` does not assert that the program compiles but crashes at
runtime. To assert runtime exceptions, use the `crash` modifier.

### Crash

The `crash` modifier asserts that the code block throws an exception at runtime


Before:

````
```scala mdoc:crash
val y = ???
```
````

After:

````
```scala
val y = ???
// scala.NotImplementedError: an implementation is missing
// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:284)
// 	at repl.Session.$anonfun$app$1(readme.md:8)
// 	at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:12)
```
````

### Passthrough

The `passthrough` modifier collects the stdout and stderr output from the
program and embeds it verbatim in the markdown file.


Before:

````
```scala mdoc:passthrough
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


After:

````
This will be rendered as markdown.

* Bullet 1
* Bullet 2

Look at the table:

|  1 |  2 |  3 |  4 |
|  2 |  4 |  6 |  8 |
|  3 |  6 |  9 | 12 |
|  4 |  8 | 12 | 16 |
````


## Key features

### Performance

Mdoc is designed to provide a tight edit/render/preview feedback loop while
writing documentation. Mdoc achieves good performance through

- [script semantics](#script-semantics): each markdown file compiles into a
  single Scala program that executes in one run.
- being incremental: with `--watch`, Mdoc compiles individual files as they
  change avoiding unnecessary work re-generating the full site.
- keeping the compiler hot: with `--watch`, Mdoc re-uses the same Scala compiler
  instance for subsequent runs making compilation faster after a few iterations.
  A medium sized document can go from compiling in ~5 seconds with a cold
  compiler down to 500ms with a hot compiler.

### Good error messages

Mdoc tries to report helpful error messages when things go wrong. Here below,
the program that is supposed to compile successfully but it has a type error so
the build is stopped with an error message from the Scala compiler.


Before:

````
```scala mdoc
val typeError: Int = "should be int"
```
````

Error:

````
error: readme.md:2:22: error: type mismatch;
 found   : String("should be int")
 required: Int
val typeError: Int = "should be int"
                     ^^^^^^^^^^^^^^^
````

Here below, the programs are supposed to fail due to the `fail` and `crash`
modifiers but they succeed so the build is stopped with an error message from
Mdoc.


Before:

````
```scala mdoc:fail
val noFail = "success"
```
```scala mdoc:crash
val noCrash = "success"
```
````

Error:

````
error: readme.md:2:1: error: Expected compile error but statement type-checked successfully
val noFail = "success"
^^^^^^^^^^^^^^^^^^^^^^
error: readme.md:5:1: error: Expected runtime exception but program completed successfully
val noCrash = "success"
^^^^^^^^^^^^^^^^^^^^^^^
````

Observe that positions of the reported diagnostics point to line numbers and
columns in the original markdown document. Internally, Mdoc instruments code
fences to extract metadata like variable types and runtime values. Positions of
error messages in the instrumented code are translated into positions in the
markdown document.

### Link hygiene

Docs get quickly out date, in particular links to different sections. After
generating a site, Mdoc analyzes links for references to non-existent sections.
For the example below, Mdoc reports a warning that the `doesnotexist` link is
invalid.


Before:

````
# My title

Link to [my title](#my-title).
Link to [old section](#doesnotexist).
````

Error:

````
warning: readme.md:4:9: warning: Section '#doesnotexist' does not exist
Link to [old section](#doesnotexist).
        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
````


### Script semantics

Mdoc interprets code fences as normal Scala programs instead of as if they're
evaluated in the REPL. This behavior is different from tut that interprets
statements as if they were typed in a REPL session. Using "script semantics"
instead of "repl semantics" has both benefits and downsides.

**Downside**: It's not possible to bind the same variable twice, for example the
code below input fails compilation with Mdoc but compiles successfully with tut

````
```scala mdoc
val x = 1
val x = 1
```
````

**Upside**: Code examples can be copy-pasted into normal Scala programs and
compile.

**Upside**: Companion objects Just Work™️


Before:

````
```scala mdoc
case class User(name: String)
object User {
  implicit val ordering: Ordering[User] = Ordering.by(_.name)
}
List(User("John"), User("Susan")).sorted
```
````

After:

````
```scala
case class User(name: String)

object User { implicit val ordering: Ordering[User] = Ordering.by(_.name) }

List(User("John"), User("Susan")).sorted
// res0: List[User] = List(User("John"), User("Susan"))
```
````


## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!

## --help

```
Mdoc v0.3.1
Usage:   mdoc [<option> ...]
Example: mdoc --in <path> --out <path> (customize input/output directories)
         mdoc --watch                  (watch for file changes)
         mdoc --site.VERSION 1.0.0     (pass in site variables)
         mdoc --exclude-path <glob>    (exclude files matching patterns)

Mdoc is a documentation tool that interprets Scala code examples within markdown
code fences allowing you to compile and test documentation as part your build. 

Common options:

  --in | -i <path> (default: "docs")
    The input directory containing markdown and other documentation sources.
    Markdown files will be processed by mdoc while other files will be copied
    verbatim to the output directory.

  --out | -o <path> (default: "out")
    The output directory to generate the mdoc site.

  --watch | -w
    Start a file watcher and incrementally re-generate the site on file save.

  --test
    Instead of generating a new site, report an error if generating the site would
    produce a diff against an existing site. Useful for asserting in CI that a
    site is up-to-date.

  --classpath String (default: "")
    Classpath to use when compiling Scala code examples. Defaults to the current
    thread's classpath.

  --site Map[String, String] (default: {})
    Key/value pairs of variables to replace through @VAR@. For example, the flag
    '--site.VERSION 1.0.0' will replace appearances of '@VERSION@' in markdown
    files with the string 1.0.0

  --clean-target
    Remove all files in the outout directory before generating a new site.

Less common options:

  --help
    Print out a help message and exit

  --usage
    Print out usage instructions and exit

  --version
    Print out the version number and exit

  --include-path [<glob> ...] (default: [])
    Glob to filter which files from --in directory to include.

  --exclude-path [<glob> ...] (default: [])
    Glob to filter which files from --in directory to exclude.

  --report-relative-paths
    Use relative filenames when reporting error messages. Useful for producing
    consistent docs on a local machine and CI. 

  --charset Charset (default: "UTF-8")
    The encoding to use when reading and writing files.

  --cwd <path> (default: "<current working directory>")
    The working directory to use for making relative paths absolute.


```

