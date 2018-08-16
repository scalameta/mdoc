# Vork - markdown worksheets

Vork is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, Vork interprets Scala code
examples in markdown files allowing you to compile markdown documentation as
part of your build. Distinguishing features of Vork include:

- fast edit/preview workflow: each markdown file is compiled into a single
  source file and executed as a single program.
- variable injection: use variables like `@VERSION@` to make sure the
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
libraryDependencies += "com" % "lib" % "@MY_VERSION@"
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
$ coursier launch com.geirsson:vork_2.12:<VERSION> -- --site.MY_VERSION 1.0.0
info: Compiling docs/readme.md
info:   done => out/readme.md (120 ms)
```

### Library

Add the following dependency to your build

```scala
// build.sbt
libraryDependencies += "com.geirsson" %% "vork" % "<VERSION>"
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

## Modifiers

Vorks supports several modifiers to control the output of code fences.

### Default

The default modifier compiles and executes the code fence as normal


Before:

````
```scala vork
val x = 1
val y = 2
x + y
```
````

After:

````
```scala
@ val x = 1
x: Int = 1

@ val y = 2
y: Int = 2

@ x + y
res0: Int = 3
```
````


### Fail

The `fail` modifier asserts that the code block will not compile


Before:

````
```scala vork:fail
val x: Int = ""
```
````

After:

````
```scala
@ val x: Int = ""
type mismatch;
 found   : String("")
 required: Int
val x: Int = ""
             ^
```
````

Note that `fail` does not assert that the program compiles but crashes at
runtime. To assert runtime exceptions, use the `crash` modifier.

### Crash

The `crash` modifier asserts that the code block throws an exception at runtime


Before:

````
```scala vork:crash
val y = ???
```
````

After:

````
```scala
val y = ???
scala.NotImplementedError: an implementation is missing
	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:284)
	at repl.Session.$anonfun$app$1(/Users/ollie/dev/vork/docs/readme.md:8)
	at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:12)
```
````

### Passthrough

The `passthrough` modifier collects the stdout and stderr output from the
program and embeds it verbatim in the markdown file.


Before:

````
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


## Error messages

Vork tries to report helpful error messages when things go wrong


Before:

````
```scala vork:fail
val noFail = "success"
```
```scala vork:crash
val noCrash = "success"
```
````

Error:

````
error: /Users/ollie/dev/vork/docs/readme.md:2:1: error: Expected compile error but statement type-checked successfully
val noFail = "success"
^^^^^^^^^^^^^^^^^^^^^^
error: /Users/ollie/dev/vork/docs/readme.md:5:1: error: Expected runtime exception but program completed successfully
val noCrash = "success"
^^^^^^^^^^^^^^^^^^^^^^^
````

## Script semantics

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

**Downside**: Vork is not a drop-in replacement for tut. If you have an existing
tut-site that relies on REPL semantics then the code examples need to be
refactored to avoid duplicate variable names.

**Upside**: Companion objects Just Work™️


Before:

````
```scala vork
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
@ case class User(name: String)

@ object User { implicit val ordering: Ordering[User] = Ordering.by(_.name) }

@ List(User("John"), User("Susan")).sorted
res0: List[User] = List(User("John"), User("Susan"))
```
````


## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!
