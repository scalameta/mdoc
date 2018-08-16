# Vork - markdown worksheets

Vork is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, Vork interprets Scala code
examples in markdown files allowing you to compile markdown documentation as
part of your build. Distinguishing features of Vork include:

- fast edit/preview workflow: each markdown file is compiled into a single
  source file and executed as a single program.
- variable injection: use variables like `@@VERSION@` to make sure the
  documentation always shows the latest version.
- extensible: library APIs expose hooks to customize rendering of code fences.
- good error messages: compile errors and crashes are reported with positions of
  the original markdown source.

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
@ val x = 1
x: Int = 1

@ List(x, x)
res0: List[Int] = List(1, 1)
```
````

Observe that `MY_VERSION` has been replaced with `1.0.0` and that the
`scala vork` code fence has been interpreted by the Scala compiler.

### Command-line

First, install the
[coursier command-line interface](https://github.com/coursier/coursier/#command-line).

```
$ coursier launch com.geirsson:vork_2.12:@VERSION@ -- --site.MY_VERSION 1.0.0
info: Compiling docs/readme.md
info:   done => out/readme.md (120 ms)
```

### Library

Add the following dependency to your build

```scala
// build.sbt
libraryDependencies += "com.geirsson" %% "vork" % "@VERSION@"
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

## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!
