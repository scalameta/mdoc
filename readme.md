# Vork - markdown worksheets

Vork is a tool to turn markdown into markdown with Scala code examples evaluated, site variables such as version numbers expanded, and links checked against 404s.
Vork is greatly inspired by [tut](https://github.com/tpolecat/tut), but distinguishes itself by:

* using [Ammonite](http://ammonite.io/) instead of the default Scala compiler REPL, enabling powerful features such as importing libraries on the fly and prettier formatted output.
* replacing variables such as `![version]` with the configured version of your project, implemented using standard markdown AST processing as opposed to regex search and replace.
* (on the roadmap) supporting single page markdown generation from multiple pages to generate PDF and EPUB files with pandoc.
* (on the roadmap) checking cross-section and external links for 404s
* (on the roadmap) rendering instant preview in VS Code as you type

Like tut, vork generates markdown files and supports a number of modifiers such as `:fail` and `:passthrough`.

## Team

The current maintainers (people who can merge pull requests) are:

* Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
* Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!
