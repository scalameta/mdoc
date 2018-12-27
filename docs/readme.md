# mdoc: compiled markdown documentation

[![Build Status](https://travis-ci.org/olafurpg/mdoc.svg?branch=master)](https://travis-ci.org/olafurpg/mdoc)
[![Join the chat at https://gitter.im/olafurpg/mdoc](https://badges.gitter.im/olafurpg/mdoc.svg)](https://gitter.im/olafurpg/mdoc)

mdoc is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, mdoc interprets Scala code
examples in markdown files allowing you to compile markdown documentation as
part of your build. Distinguishing features of mdoc include:

- [good performance](#performance): incremental, hot compilation gives you
  snappy feedback while writing documentation.
- [program semantics](#program-semantics): markdown documents are compiled into
  normal Scala programs making examples copy-paste friendly.
- [good error messages](#good-error-messages): compile errors and crashes are
  reported with positions of the original markdown source making it easy to
  track down where things went wrong.
- [link hygiene](#link-hygiene): catch broken links to non-existing sections
  while generating the site.
- [variable injection](#variable-injection): instead of hardcoding constants
  like versions numbers, use variables like `@@VERSION@` to make sure the
  documentation stays up-to-date with new releases.
- [extensible](#extensible): library APIs expose hooks to customize rendering of
  code fences.

Table of contents:

<!-- TOC depthFrom:2 -->

- [Quickstart](#quickstart)
    - [Library](#library)
    - [sbt](#sbt)
    - [Command-line](#command-line)
- [Modifiers](#modifiers)
    - [Default](#default)
    - [Silent](#silent)
    - [Fail](#fail)
    - [Crash](#crash)
    - [Passthrough](#passthrough)
    - [Invisible](#invisible)
    - [Reset](#reset)
    - [PostModifier](#postmodifier)
    - [StringModifier](#stringmodifier)
    - [Scastie](#scastie)
- [Key features](#key-features)
    - [Performance](#performance)
    - [Good error messages](#good-error-messages)
    - [Link hygiene](#link-hygiene)
    - [Program semantics](#program-semantics)
    - [Variable injection](#variable-injection)
    - [Extensible](#extensible)
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

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_@SCALA_BINARY_VERSION@/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_@SCALA_BINARY_VERSION@)

```scala
// build.sbt
scalaVersion := "@SCALA_VERSION@" // Any version in @SCALA_BINARY_VERSION@.x works.
libraryDependencies += "com.geirsson" %% "mdoc" % "@VERSION@"
```

Then write a main function that invokes mdoc as a library

```scala
object Main {
  def main(args: Array[String]): Unit = {
    // build arguments for mdoc
    val settings = mdoc.MainSettings()
      .withSiteVariables(Map("MY_VERSION" -> "1.0.0"))
      .withArgs(args.toList)
    // generate out/readme.md from working directory
    val exitCode = mdoc.Main.process(settings)
    // (optional) exit the main function with exit code 0 (success) or 1 (error)
    if (exitCode != 0) sys.exit(exitCode)
  }
}
```

Consult [--help](#--help) to see what arguments are valid for `withArgs`.

Consult the mdoc source to learn more how to use the library API. Scaladocs are
available
[here](https://www.javadoc.io/doc/com.geirsson/mdoc_@SCALA_BINARY_VERSION@/@VERSION@)
but beware there are limited docstrings for classes and methods. Keep in mind
that code in the package `mdoc.internal` is subject to binary and source
breaking changes between any release, including PATCH versions.

### sbt

There is no sbt plugin for mdoc, create a new project instead that uses the mdoc
[Library API](#library).

```scala
// build.sbt
lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
    moduleName := "myproject-docs",
    scalaVersion := "@SCALA_VERSION@", // Any version in @SCALA_BINARY_VERSION@.x works.
    libraryDependencies += "com.geirsson" %% "mdoc" % "@VERSION@",
    // (optional): enable compiler plugins and other flags
    resourceGenerators.in(Compile) += Def.task {
      val out = resourceDirectory.in(Compile).value / "mdoc.properties"
      val props = new java.util.Properties()
      props.put("scalacOptions", scalacOptions.in(Compile).value.mkString(" "))
      IO.write(props, "mdoc properties", out)
      List(out)
    }
  )
  .dependsOn(myproject)
```

From the sbt shell, run the mdoc main function

```scala
// sbt shell
> docs/runMain mdoc.Main --help
```

### Command-line

First, install the
[coursier command-line interface](https://github.com/coursier/coursier/#command-line).
Then run the following command:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_@SCALA_BINARY_VERSION@/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_@SCALA_BINARY_VERSION@)

```
$ coursier launch com.geirsson:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- --site.MY_VERSION 1.0.0
info: Compiling docs/readme.md
info:   done => out/readme.md (120 ms)
```

It's possible to customize the input and output directory with `--in` and
`--out`. There is also a `--watch` flag to watch for file changes and
incrementally re-generate pages on file save providing fast feedback while
Consult [`--help`](#--help) to learn more about the command-line interface.

## Modifiers

mdocs supports several modifiers to control the output of code fences.

### Default

The default modifier compiles and executes the code fence as normal

````scala mdoc:mdoc
```scala mdoc
val x = 1
val y = 2
x + y
```
````

### Silent

The `silent` modifier is identical to the default modifier except that it hides
the evaluated output. The input code fence renders unchanged.

````scala mdoc:mdoc
```scala mdoc:silent
val x = 1
val y = 2
x + y
```
```scala mdoc
x + y
```
````

### Fail

The `fail` modifier asserts that the code block will not compile

````scala mdoc:mdoc
```scala mdoc:fail
val x: Int = ""
```
````

Note that `fail` does not assert that the program compiles but crashes at
runtime. To assert runtime exceptions, use the `crash` modifier.

### Crash

The `crash` modifier asserts that the code block throws an exception at runtime

````scala mdoc:mdoc
```scala mdoc:crash
val y = ???
```
````

### Passthrough

The `passthrough` modifier collects the stdout and stderr output from the
program and embeds it verbatim in the markdown file.

````scala mdoc:mdoc
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

### Invisible

The `invisible` modifier evaluates the code but does not render anything. The
`invisible` modifier is equivalent to `passthrough` when the expression does not
print to stdout.

````scala mdoc:mdoc
This is prose.
```scala mdoc:invisible
println("I am invisible")
```
More prose.
````

### Reset

The `reset` modifier starts a new scope where previous statements in the
document are no longer available. This can be helpful to clear existing imports
or implicits in scope.

````scala mdoc:mdoc
```scala mdoc
implicit val x: Int = 41
```

```scala mdoc:reset
implicit val y: Int = 42
implicitly[Int] // x is no longer in scope
```
```scala mdoc:fail
println(x)
```
````

### PostModifier

A `PostModifier` is a custom modifier that post-processes a compiled and
interpreted mdoc code fence. Post modifiers have access to the original code
fence text, the static types and runtime values of the evaluated Scala code, the
input and output file paths and other contextual information.

One example use-case for post modifiers is to render charts based on the runtime
value of the last expression in the code fence.

![](evilplot.gif)

Extend the `mdoc.PostModifier` trait to implement a post modifier.

```scala mdoc:file:mdoc-docs/src/main/scala/mdoc/docs/EvilplotModifier.scala

```

Next, create a resource file `META-INF/services/mdoc.PostModifier` so the post
modififer is recognized by the JVM
[ServiceLoader](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html)
framework.

```scala mdoc:file:mdoc-docs/src/main/resources/META-INF/services/mdoc.PostModifier

```

As long as `EvilplotModifier` is available on the classpath, for example via
`libraryDependencies` in build.sbt, then you can use the modifier like this.

````scala mdoc:mdoc
```scala mdoc:evilplot:scatterplot.png
import com.cibo.evilplot._
import com.cibo.evilplot.plot._
import com.cibo.evilplot.plot.aesthetics.DefaultTheme._
import com.cibo.evilplot.numeric.Point

val data = Seq.tabulate(90) { i =>
  val degree = i * 8
  val radian = math.toRadians(degree)
  Point(i.toDouble, math.sin(radian))
}

ScatterPlot(data)
  .xAxis()
  .yAxis()
  .frame()
  .xLabel("x")
  .yLabel("y")
  .render()
```
````

Which renders into a scatter plot like this:

![](scatterplot.png)

It's important that post modifiers present helpful error messages to the user in
case of failures. For example, if the last runtime value is not an EvilPlot
`Drawable` we can report the expected and obtained types with carets pointing to
the position of the last variable.

````scala mdoc:mdoc:crash
```scala mdoc:evilplot:scatterplot.png
val message = "hello world!"
```
````

### StringModifier

A `StringModifier` is a custom modifier that processes the plain text contents
of a code block, ignoring the compilation and interpretation of the Scala code.

```scala mdoc:silent
import mdoc.StringModifier
import mdoc.Reporter
import scala.meta.Input
class FooModifier extends StringModifier {
  override val name = "foo"
  override def process(info: String, code: Input, reporter: Reporter): String = {
    val originalCodeFenceText = code.text
    val isCrash = info == "crash"
    if (isCrash) "BOOM"
    else "OK: " + originalCodeFenceText
  }
}
```

Pass the custom modifier to `MainSettings.withStringModifiers(List(...))`.

```scala
val settings = mdoc.MainSettings()
  .withStringModifiers(List(
    new FooModifier
  ))
```

Code blocks with the `mdoc:foo` modifier will then render as follows.

````scala mdoc:mdoc
```scala mdoc:foo
Hello world!
```
````

We can also add the argument `:crash` to render "BOOM".

````scala mdoc:mdoc
```scala mdoc:foo:crash
Hello world!
```
````

### Scastie

The `scastie` modifier transforms a Scala code block into a
[Scastie](https://scastie.scala-lang.org/) snippet.

> ℹ️ This modifier will work only in environments that support embedding a
> `<script>` tag. For example, it won't work in GitHub readmes, but it will work
> when building a static website from Markdown (e.g., with
> [Docusaurus](https://docusaurus.io/))

You can embed an existing Scastie snippet by its id:

````scala mdoc:mdoc
```scala mdoc:scastie:xbrvky6fTjysG32zK6kzRQ

```
````

or in case of a user's snippet:

````scala mdoc:mdoc
```scala mdoc:scastie:MasseGuillaume/CpO2s8v2Q1qGdO3vROYjfg

```
````

> ⚠️ The empty line in the block can't be omitted due to how the Markdown parser
> works

Moreover, you can quickly translate any Scala code block block into a Scastie
snippet on the fly.

````scala mdoc:mdoc
```scala mdoc:scastie
val x = 1 + 2
println(x)
```
````

> ⚠️ Inline snippets are slower to run than embedded ones, since they won't be
> cached. You should prefer embedding existing snippets whenever possible.

You can choose the Scastie theme when initializing the Scastie modifier:

```scala mdoc
import mdoc.modifiers.ScastieModifier
new ScastieModifier(theme = "dark") // default is "light"
```

## Key features

### Performance

mdoc is designed to provide a tight edit/render/preview feedback loop while
writing documentation. mdoc achieves good performance through

- [program semantics](#program-semantics): each markdown file compiles into a
  single Scala program that executes in one run.
- being incremental: with `--watch`, mdoc compiles individual files as they
  change avoiding unnecessary work re-generating the full site.
- keeping the compiler hot: with `--watch`, mdoc re-uses the same Scala compiler
  instance for subsequent runs making compilation faster after a few iterations.
  A medium sized document can go from compiling in ~5 seconds with a cold
  compiler down to 500ms with a hot compiler.

### Good error messages

mdoc tries to report helpful error messages when things go wrong. Here below,
the program that is supposed to compile successfully but it has a type error so
the build is stopped with an error message from the Scala compiler.

````scala mdoc:mdoc:crash
```scala mdoc
val typeError: Int = "should be int"
```
````

Here below, the programs are supposed to fail due to the `fail` and `crash`
modifiers but they succeed so the build is stopped with an error message from
mdoc.

````scala mdoc:mdoc:crash
```scala mdoc:fail
val noFail = "success"
```
```scala mdoc:crash
val noCrash = "success"
```
````

Observe that positions of the reported diagnostics point to line numbers and
columns in the original markdown document. Internally, mdoc instruments code
fences to extract metadata like variable types and runtime values. Positions of
error messages in the instrumented code are translated into positions in the
markdown document.

### Link hygiene

Docs get quickly out date, in particular links to different sections. After
generating a site, mdoc analyzes links for references to non-existent sections.
For the example below, mdoc reports a warning that the `doesnotexist` link is
invalid.

```scala mdoc:mdoc:crash
# My title

Link to [my title](#my-title).
Link to [typo section](#mytitle).
Link to [old section](#doesnotexist).
```

Observe that mdoc suggests a fix if there exists a header that is similar to the
unknown link.

### Program semantics

mdoc interprets code fences as normal Scala programs instead of using the REPL.
This behavior is different from tut that interprets statements as if they were
typed in a REPL session. Using "program semantics" instead of "repl semantics"
has benefits and downsides.

**Downside**: It's not possible to bind the same variable twice, for example the
code below fails compilation with mdoc but compiles successfully with tut

````
```scala mdoc
val x = 1
val x = 1
```
````

**Upside**: Code examples from the documentation can be copy-pasted into normal
Scala programs and compile.

**Upside**: Companion objects work as expected.

````scala mdoc:mdoc
```scala mdoc
case class User(name: String)
object User {
  implicit val ordering: Ordering[User] = Ordering.by(_.name)
}
List(User("Susan"), User("John")).sorted
```
````

**Upside**: Overloaded methods work as expected.

````scala mdoc:mdoc
```scala mdoc
def add(a: Int, b: Int): Int = a + b
def add(a: Int): Int = add(a, 1)
add(3)
```
````

**Upside**: Mutually recursive methods work as expected.

````scala mdoc:mdoc
```scala mdoc
def isEven(n: Int): Boolean = n == 0 || !isOdd(n - 1)
def isOdd(n: Int): Boolean  = n == 1 || !isEven(n - 1)
isEven(8)
```
````

**Upside**: Compiler options like `-Ywarn-unused` don't report spurious errors like they do in the REPL.
```scala
$ scala -Ywarn-unused
scala> import scala.concurrent.Future
<console>:11: warning: Unused import
       import scala.concurrent.Future
                               ^
scala> Future.successful(1)
res0: scala.concurrent.Future[Int] = Future(Success(1))
```

### Variable injection

mdoc renders variables like `@@VERSION@` into `@VERSION@`. This makes it easy to
keep documentation up-to-date as new releases are published. Variables can be
passed from the command-line interface with the syntax

```
mdoc --site.VERSION 1.0.0 --site.SCALA_VERSION @SCALA_VERSION@
```

When using the library API, variables are passed with the
`MainSettings.withSiteVariables(Map[String, String])` method

```scala
val settings = mdoc.MainSettings()
  .withSiteVariables(Map(
    "VERSION" -> "1.0.0",
    "SCALA_VERSION" -> "@SCALA_VERSION@"
  ))
```

Variables are replaced with the regular expression `@(\w+)@`. An error is
reported when a variable that matches this pattern is not provided.

```scala mdoc:mdoc:crash
Install version @@DOES_NOT_EXIST@
```

Use double `@@` to escape variable injection.

```scala mdoc:mdoc
Install version @@@OLD_VERSION@
```

### Extensible

The mdoc library API enables users to implement "modifiers" that customize the
rendering of mdoc code fences. There are two kinds of modifiers:

- [PostModifier](#postmodifier): post-process a compiled and interpreted mdoc
  code fence.
- [StringModifier](#stringmodifier): process code fences as plain string values
  without mdoc compilation or intepretation.

⚠️ This feature is under development and is likely to have breaking changes in
future releases.

## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!

## --help

````scala mdoc:passthrough
println("```")
println(mdoc.internal.cli.Settings.help(mdoc.docs.Docs.stableVersion, 80))
println("```")
````
