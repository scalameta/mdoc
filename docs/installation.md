---
id: installation
title: Installation
---

Create a `docs` directory and `docs/readme.md` file with some basic content.

````
$ tree
.
└── docs
    └── readme.md
$ cat docs/readme.md
# My Project

To install my project
```scala
libraryDependencies += "com" % "lib" % "@@VERSION@"
```

```scala mdoc
val x = 1
List(x, x)
```
````

We process the markdown with one of the following mdoc integrations:

- [sbt-mdoc](#sbt): for usage with sbt projects.
- [command-line](#command-line): to run from the console outside of a build
  tool.
- [library API](#library): for programmatic usage.

The generated `readme.md` will look like this.

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

Observe that `VERSION` has been replaced with `1.0.0` and that the `scala mdoc`
code fence has been evaluated.

## sbt

Install the `sbt-mdoc` plugin and create a `docs` project in build.sbt that
enables `mdoc.MdocPlugin`.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scalameta/mdoc_@SCALA_BINARY_VERSION@/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scalameta/mdoc_@SCALA_BINARY_VERSION@)

```scala
// project/plugins.sbt
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "@VERSION@" )
// build.sbt
lazy val myproject = project  // your existing library
  .settings(...)
lazy val docs = project       // new documentation project
  .in(file("myproject-docs")) // important: it must not be docs/
  .dependsOn(myproject)
  .enablePlugins(MdocPlugin)
```

Next, from the sbt shell, run the `docs/mdoc` task to generate the documentation
site. By default, the `mdoc` task looks for markdown sources in the toplevel
`docs/` directory.

```scala
// sbt shell
> docs/mdoc
```

Update `mdocVariables` to include site variables like `@@VERSION@`.

```diff
// build.sbt
lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
+   mdocVariables := Map(
+     "VERSION" -> version.value
+   )
  )
  .dependsOn(myproject)
  .enablePlugins(MdocPlugin)
```

The `mdoc` task runs the mdoc command-line interface so it's possible to pass in
arguments like `--watch` to start file watcher with livereload. It's recommended
to use `--watch` while writing documentation to enjoy 3-4x faster compilation
performance.

```scala
> docs/mdoc --watch
```

See [`--help`](#help) to learn more how to use the command-line interface.

```scala
> docs/mdoc --help
```

### Reference

The sbt-mdoc plugin supports the following settings.

```scala mdoc:sbt

```

## Command-line

Use [coursier](https://github.com/coursier/coursier/#command-line) to launch
mdoc outside of a build tool.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scalameta/mdoc_@SCALA_BINARY_VERSION@/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scalameta/mdoc_@SCALA_BINARY_VERSION@)

```sh
curl -L -o coursier https://git.io/coursier
chmod +x coursier
coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- --site.VERSION 1.0.0
info: Compiling 1 file to website/target/docs
info: Compiled in 1.2s (0 errors)
```

Add libraries to the launched classpath to include them for compilation.

```diff
 coursier launch \
   org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ \
+  org.typelevel:cats-core_@SCALA_BINARY_VERSION@:1.5.0
```

Use `--in` to customize the input directory where markdown sources are
contained, by default the `docs/` directory is used.

```diff
 coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --in mydocs
```

The `--in` flag doesn't have to be a directory, it also supports individual
files.

```diff
 coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --in mydocs/readme.md
```

Use `--site.VARIABLE=value` to add site variables that can be referenced from
markdown as `@@VARIABLE@`.

```diff
 coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --site.SCALA_VERSION @SCALA_VERSION@
```

Use `--out` to customize where your markdown sources are generated, by default
the `out/` directory is used.

```diff
 coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --out target/docs
```

The `--out` flag doesn't have to be a directory, it can also be an individual
file. However, this assumes that your `--in` was also an individual file.

```diff
 coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --in mydocs/readme.template.md \
+  --out readme.md
```

Use `--watch` to start the file watcher with livereload. It's recommended to use
`--watch` while writing documentation to enjoy 3-4x faster compilation
performance.

```diff
 coursier launch org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --watch
```

### Help

Use `--help` to learn more how to use the command-line interface.

````scala mdoc:passthrough
println("```txt")
println(mdoc.internal.cli.Settings.help(mdoc.docs.Docs.stableVersion, 80))
println("```")
````

## Library

Add the following dependency to your build

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scalameta/mdoc_@SCALA_BINARY_VERSION@/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scalameta/mdoc_@SCALA_BINARY_VERSION@)

```scala
// build.sbt
scalaVersion := "@SCALA_VERSION@" // Any version in @SCALA_BINARY_VERSION@.x works.
libraryDependencies += "org.scalameta" %% "mdoc" % "@VERSION@"
```

Then write a main function that invokes mdoc as a library

It's recommended to use `--watch` while writing documentation to enjoy 3-4x
faster compilation performance.

```scala mdoc:silent
object Main {
  def main(args: Array[String]): Unit = {
    // build arguments for mdoc
    val settings = mdoc.MainSettings()
      .withSiteVariables(Map("VERSION" -> "1.0.0"))
      .withArgs(args.toList)
    // generate out/readme.md from working directory
    val exitCode = mdoc.Main.process(settings)
    // (optional) exit the main function with exit code 0 (success) or 1 (error)
    if (exitCode != 0) sys.exit(exitCode)
  }
}
```

If you use sbt-mdoc, update the `mdoc` task to call `run` instead of the default
`runMain mdoc.Main`.

```diff
// build.sbt
lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
+   mdoc := run.in(Compile).evaluated
  )
  .dependsOn(myproject)
  .enablePlugins(MdocPlugin)
```

Consult the mdoc source to learn more how to use the library API. Scaladocs are
available
[here](https://www.javadoc.io/doc/org.scalameta/mdoc_@SCALA_BINARY_VERSION@/@VERSION@)
but beware there are limited docstrings for classes and methods. Keep in mind
that code in the package `mdoc.internal` is subject to binary and source
breaking changes between any release, including PATCH versions.
