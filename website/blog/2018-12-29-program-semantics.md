---
author: Ólafur Páll Geirsson
title: Fast typechecked markdown documentation with clear error messages
authorURL: https://twitter.com/olafurpg
authorImageURL: https://avatars2.githubusercontent.com/u/1408093?s=460&v=4
---

This post introduces how mdoc evaluates Scala code examples with good
performance while reporting clear error messages. mdoc is a markdown
documentation tool inspired by [tut](http://tpolecat.github.io/tut/).

Like tut, mdoc reads markdown files as input and produces markdown files as
output with the Scala code examples evaluated. Unlike tut, mdoc does not use the
Scala REPL to evaluate Scala code examples. Instead, mdoc translates each
markdown file into a regular Scala program that evaluates in one run. In this
post, we learn how this approach delivers improved performance.

<!-- truncate -->

## REPL semantics

It's not possible to write companion objects or overloaded methods with tut.
This limitation comes from how the Scala REPL evaluates each statement
individually.

```scala
scala> case class User(name: String)
scala> object User {
     |   implicit val ordering: Ordering[User] = Ordering.by(_.name)
     | }
defined object User
warning: previously defined class User is not a companion to object User.
Companions must be defined together; you may wish to use :paste mode for this.
```

The warning may be surprising if you are not familiar with how the REPL works.
In the REPL, each statements gets wrapped into a synthetic object. We can look
at the generated code by adding the compiler option `-Xprint:parser`.

```scala
$ scala -Xprint:parser
scala> case class User(name: String)
package $line3 {
  // ..
  case class User(...)
}
scala> object User
package $line4 {
  // ..
  object User
}
```

It's not possible for `object User` to be a companion of `class User` because
they're defined in separate objects `$line3` and `$line4`. This encoding is
required for the REPL because we don't know ahead-of-time how many statements
will get evaluated in the session.

## Program semantics

Instead of evaluating each statement individually like in the REPL, mdoc builds
a single Scala program that evaluates in one run. The approach is possible
because we know which statements appear in the document, unlike the REPL. For
example, consider the following markdown document.

````md
```scala mdoc
val x = 1
```

```scala mdoc
println(x)
```
````

This document gets translated by mdoc into roughly the following instrumented
Scala program.

```scala
class Session extends mdoc.DocumentBuilder {
  def app(): Unit = {
    new App()
  }
  class App() {
    super.$doc.startStatement()
    val x = 1                     ; super.$doc.binder(x)
    super.$doc.endStatement()
    super.$doc.startStatement()
    val res0 = println(x)         ; super.$doc.binder(res0)
    super.$doc.endStatement()
  }
}
```

The `$doc.startStatement()` and `$doc.binder()` instrumentation captures
variable types, runtime values and standard output from evaluating each
statement.

When `Session.app()` is evaluated, the mdoc instrumentation builds up a data
structure with enough information to render the document back into markdown.

There are many benefits to using this approach over the REPL:

- better performance: the document is compiled once and classloaded once instead
  of compiled + classloaded per-statement.
- language features like companion objects, overloaded methods and mutually
  recursive methods just work.
- the mdoc instrumentation builds a rich data structure giving us fine-grained
  control over pretty-printing of static types and runtime values.

However, one challenge with the approach is that error messages point to cryptic
positions in the instrumented code instead of the original markdown source.

## Clear error messages

It's annoying if compile errors show synthetic code that you didn't write
yourself.

```scala
// error: generated/Session.scala:45:10 type mismatch;
//   obtained: Int
//   expected: String
val res0 = "hello".matches("h".length) ; super.$doc.binder(res0)
                           ^
```

We want compile errors to point to the original markdown source instead.

```scala
// error: readme.md:10:8 type mismatch;
//   obtained: Int
//   expected: String
"hello".matches("h".length)
                ^^^^^^^^^^
```

To report readable error messages, mdoc translates positions in the synthetic
program to positions in the markdown source. To translate positions, mdoc
tokenizes the original source code and the synthetic source code and aligns the
tokens using [edit-distance](https://en.wikipedia.org/wiki/Edit_distance).

```diff
-val
-res0
-=
 "hello"
 .
 matches
 (
 "h"
 .
 length
 )
- ;
- super
- .
- $doc
- .
- binder
- (
- res0
- )
```

Edit-distance is used by tools like `git` and `diff` to show which lines have
changed between two source files. Instead of comparing textual lines, mdoc
compares tokens.

As long as the instrumented code doesn't contain type errors, all error messages
reported by the compiler should translate to positions in the original markdown
source.

## Evaluation

Let's test mdoc by running it on the [http4s](https://github.com/http4s/http4s)
documentation. Http4s is a minimal, idiomatic Scala interface for HTTP.

![http4s GitHub respository](https://user-images.githubusercontent.com/1408093/50538581-e1dbd000-0b71-11e9-830a-a958a5c75dce.png)

We start by cloning the http4s repository and install the sbt-mdoc plugin.

```sh
git clone https://github.com/http4s/http4s
cd http4s
```

```diff
// project/plugins.sbt
+ addSbtPlugin("com.geirsson" % "sbt-mdoc" % "<VERSION>")
// build.sbt
lazy val docs = project
  .enablePlugins(
+   MdocPlugin
  )
  .settings(
+   mdocIn := tutSourceDirectory.value
  )
```

Next, we run the tut migration script.

````sh
find . -name '*.md' -type f -exec perl -pi -e '
  s/```tut:book/```scala mdoc/g;
  s/```tut/```scala mdoc/g;
' {} +
````

Then we run `sbt docs/mdoc` and get a lot of compile errors.

```scala
> docs/mdoc
...
error: docs/src/main/tut/client.md:274:5: error: meteredClient is already defined as value meteredClient
val meteredClient = Metrics[IO](Prometheus(registry, "prefix"), requestMethodClassifier)(httpClient)
    ^^^^^^^^^^^^^
info: Compiled in 14.02s (46 errors, 36 warnings)
```

It's expected that there are compile errors because mdoc uses program semantics
instead of REPL semantics. For example, in this particular example
`meteredClient` was already defined in this document.

We rename a few conflicting variables and comment out two ambiguous implicits.

````diff
-```tut
-val io = Ok(IO.fromFuture(IO(Future {
+```scala mdoc
+val io2 = Ok(IO.fromFuture(IO(Future {
   println("I run when the future is constructed.")
   "Greetings from the future!"
 })))
-io.unsafeRunSync
+io2.unsafeRunSync
 ```
-implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
-  QueryParamDecoder[Int].map(Year.of)
+// implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
+//   QueryParamDecoder[Int].map(Year.of)
````

We disable fatal warnings and run mdoc again but only for the modified document
`dsl.md`.

```scala
> set scalacOptions in docs -= "-Xfatal-warnings"
> docs/mdoc --include dsl.md --watch
info: Compiling 1 file to /Users/olafurpg/dev/http4s/docs/target/mdoc
I run when the future is constructed.
143303243 nanoseconds244212367 nanoseconds348637483 nanoseconds453423189 nanoseconds557908112 nanoseconds662361125 nanoseconds767036209 nanoseconds871488475 nanoseconds975876061 nanoseconds1081563422 nanoseconds
info: Compiled in 9.74s (0 errors)
Waiting for file changes (press enter to interrupt)
```

It took 10 seconds to evaluate the document for the first time. We insert
a blank line and generate the document again and it only takes 3.5 seconds. It's
faster the second time because the JVM has warmed up a bit.

```scala
info: Compiling 1 file to /Users/olafurpg/dev/http4s/docs/target/mdoc
info: Compiled in 3.53s (0 errors)
Waiting for file changes (press enter to interrupt)
```

After 6 iterations of adding blank lines and recompiling it takes 2.4 seconds to
generate the document.

```scala
info: Compiling 1 file to /Users/olafurpg/dev/http4s/docs/target/mdoc
info: Compiled in 2.39s (0 errors)
Waiting for file changes (press enter to interrupt)
```

We introduce an intentional compile error.

```diff
- object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")
+ object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("year")
```

```
error: /Users/olafurpg/dev/http4s/docs/src/main/tut/dsl.md:449:41: error: type mismatch;
 found   : Int
 required: java.time.Year
        Ok(getAverageTemperatureForYear(year))
                                        ^^^^
info: Compiled in 0.82s (1 error, 1 warning)
Waiting for file changes (press enter to interrupt)
```

Observe that the error is reported within one second, faster than it takes to
process the document on success. The error message position points to line 449
and column 41 which is exactly where `year` identifier is referenced. In iTerm,
you can cmd+click on the error to open your editor at that position.

We compare the performance with tut by checking out the master branch and run
`docs/tutOnly dsl.md` four times.

```scala
> docs/tutOnly dsl.md
[success] Total time: 21 s, completed Dec 29, 2018 2:20:42 PM
> docs/tutOnly dsl.md
[success] Total time: 28 s, completed Dec 29, 2018 2:21:12 PM
> docs/tutOnly dsl.md
[success] Total time: 25 s, completed Dec 29, 2018 2:21:49 PM
> docs/tutOnly dsl.md
[success] Total time: 27 s, completed Dec 29, 2018 2:22:25 PM
```

We introduce the same compile error as before.

```diff
- object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")
+ object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("year")
```

```scala
[tut] *** Error reported at /Users/olafurpg/dev/http4s/docs/src/main/tut/dsl.md:458
// <console>:59: error: type mismatch;
//  found   : Int
//  required: java.time.Year
//                Ok(getAverageTemperatureForYear(year))
//                                                ^
[error] Total time: 22 s, completed Dec 29, 2018 2:39:42 PM
```

Observe that it took 22 seconds to report the compile error. Also, the position
points to line 458, which is the last line of the code fence containing the
closing `}`, but it's is not the line where `year` is referenced.

Some observations:

- it requires manual effort to translate documentation from REPL semantics to
  program semantics. The migration can't be automated and you sacrifice
  flexibility in variable naming and implicits usage by migrating to mdoc.
- for cold performance, mdoc takes 10 seconds while tut takes 21 seconds. My
  theory is that the primary reason for this difference is REPL semantics vs.
  program semantics.
- for hot performance, mdoc takes 2.4 seconds while tut takes between 21 and 28
  seconds. Under `--watch` mode, mdoc reuses the same compiler instance between
  runs it possible for the JVM to optimize the code. I suspect tut can enjoy
  similar speedups by introducing a `--watch` mode.
- mdoc reports compile errors in 0.8 seconds with exact line and column
  positions of the original markdown document, while tut reports compile errors
  in 22 seconds with accurate but not exact line numbers. The big difference
  likely comes from the fact that the REPL needs to compile + classload all
  leading statements in the document to reach the compile error while mdoc has
  no bytecode to classload when the document contains a compile error.

## Conclusion

In this post, we learned the difference between REPL semantics used by tut and
program semantics used by mdoc. Program semantics enable mdoc to process
markdown documents up to 2x faster under cold compilation, and 10x faster when
combined with `--watch` mode under hot compilation.

To report clear error messages, mdoc uses edit-distance to align tokens in the
original markdown source with tokens in the instrumented program. This technique
enables mdoc to generate instrumented code while reporting positions in the
original markdown source.

Migrating from REPL semantics to program semantics requires manual effort. If
you have a lot of documentation and want a tighter edit/preview feedback loop,
the migration might be worth your effort.

Several projects already use mdoc: mdoc itself (this website),
[Scalafmt](https://scalameta.org/scalafmt/),
[Scalafix](https://scalacenter.github.io/scalafix/),
[Scalameta](http://scalameta.org/), [Metals](https://scalameta.org/metals/),
[Bloop](https://scalacenter.github.io/bloop/),
[Coursier](https://coursier.github.io/coursier/),
[Almond](http://almond-sh.github.io/almond/stable/docs/intro) and
[fs2-kafka](https://ovotech.github.io/fs2-kafka/). Those projects may serve as
inspiration for how to integrate mdoc with your project.
