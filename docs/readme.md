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

### Command-line

First, install the
[coursier command-line interface](https://github.com/coursier/coursier/#command-line).
Then run

```
coursier launch com.geirsson:vork_2.12:@VERSION@ -- --watch
```

## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!
