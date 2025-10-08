---
id: directives
title: Using Directives
sidebar_label: Directives
---

Similar to how [Scala CLI](https://scala-cli.virtuslab.org/) works, mdoc also
supports some directives that you can use to add additional capabilities to your
snippets. The number of directives is reduced to a much smaller number, but they
should work mostly the same.

## Dependencies

To add new dependencies use the `deps` directive. The dependencies are added to
the classpath of the code block.

````scala mdoc:mdoc
```scala mdoc
//> using dep com.lihaoyi::scalatags:0.13.1
import scalatags.Text.all._
val htmlFile = html(
  body(
    p("This is a big paragraph of text")
  )
)
```
````

## Repositories

To add new repositories use the `repos` directive. These are necessary if you
want to use a dependency that is not in the default maven central repository.

````scala mdoc:mdoc
```scala mdoc
//> using repo https://repo.scala-lang.org/artifactory/maven-nightlies
//> using dep annogen:annogen:0.1.0
```
````

## Compiler Options

It's also possible to add Scala compiler options to your snippets, to do that
use the 'options' directive.

````scala mdoc:mdoc:fail
```scala mdoc:fail
//> using option -Ywarn-unused-import -Xfatal-warnings
import scala.util.Try
println(42)
```
````

## Importing files

To import files into your snippets, you can use the `files` directive. This is
useful if you want to import outside definitions from other files. The files are
compiled and added to the classpath of the code block. Currently only `.sc` script
files are supported.

````scala mdoc:mdoc:crash
```scala mdoc
//> using file ../hello.sc
println(hello.message)
```
````

## Ammonite style imports

Mdoc also support Ammonite style imports, but they are currently deprecated and
the using directives are preferred.
