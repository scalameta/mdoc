---
id: why
title: Why mdoc?
---

mdoc is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, mdoc reads markdown files as
input and produces markdown files as output. Unlike tut, mdoc does not use the
Scala REPL to evaluate Scala code. Instead, mdoc translates markdown files into
regular Scala programs and evaluates them in one run. This approach enables mdoc
to support several distinguishing features:

- [good performance](#performance): markdwn documents compile as a single Scala
  program and evaluate in one run. Combined with incremental and hot compilation
  from file watching, medium sized documents can generate in a few hundred
  milliseconds.
- [program semantics](#program-semantics): markdown documents are compiled into
  normal Scala programs making language features work as you expect them to.
- [good error messages](#good-error-messages): compile errors and crashes are
  reported with positions of the original markdown source making it easy to
  track down where things went wrong.
- [link hygiene](#link-hygiene): catch broken links to non-existing sections
  while generating the site.
- [variable injection](#variable-injection): use variables like `@@VERSION@` to
  that make sure the documentation stays up-to-date with new releases.
- [extensibility](#extensibility): custom modifiers allow you to
  programmatically control how the resulting markdown gets rendered.

## Performance

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

## Good error messages

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

## Link hygiene

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

## Program semantics

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

**Upside**: Compiler options like `-Ywarn-unused` don't report spurious errors
like they do in the REPL.

```scala
$ scala -Ywarn-unused
scala> import scala.concurrent.Future
<console>:11: warning: Unused import
       import scala.concurrent.Future
                               ^
scala> Future.successful(1)
res0: scala.concurrent.Future[Int] = Future(Success(1))
```

## Variable injection

mdoc renders variables like `@@VERSION@` into `@VERSION@`. This makes it easy to
keep documentation up-to-date as new releases are published. Variables can be
passed from the command-line interface with the syntax `--site.VARIABLE=value`.

```
mdoc --site.VERSION 1.0.0 --site.SCALA_VERSION @SCALA_VERSION@
```

When using the library API, variables are passed with the
`MainSettings.withSiteVariables(Map[String, String])` method

```diff
 val settings = mdoc.MainSettings()
+  .withSiteVariables(Map(
+    "VERSION" -> "1.0.0",
+    "SCALA_VERSION" -> "@SCALA_VERSION@"
+  ))
```

When using sbt-mdoc, variables are defined with the `mdocVariables` setting.

```diff
// build.sbt
lazy val docs = project
  .settings(
+   mdocVariables := Map(
+     "VERSION" -> "1.0.0"
+   )
  )
  .enablePlugins(MdocPlugin)
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

## Extensibility

The mdoc library APi enables you to implement custom modifiers like
[`PostModifier`](modifiers.md#postmodifier), which have access to the original
code fence text, the static types and runtime values of the evaluated Scala
code, the input and output file paths and other contextual information.

Example usages of custom modifiers:

- This website has a `mdoc:mdoc` modifier to render before/after examples of
  rendered markdown.
- The [Scalafmt website](https://scalameta.org/scalafmt/docs/configuration.html)
  has a `mdoc:scalafmt` modifier to show before/after code examples with a given
  configuration.
