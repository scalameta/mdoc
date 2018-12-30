---
id: tut
title: Coming from tut
---

This is a reference guide to migrating from tut to mdoc. It's not possible to
automatically migrate tut documentation to mdoc because mdoc uses
[program semantics](why.md#program-semantics) while tut uses REPL semantics.
This means that some tut documentation can't compile with mdoc without manual
changes.

| tut                | mdoc                                                  |
| ------------------ | ----------------------------------------------------- |
| `:fail`            | `:fail` for compile error, `:crash` for runtime error |
| `:nofail`          | n/a                                                   |
| `:silent`          | `:silent`                                             |
| `:plain`           | n/a                                                   |
| `:invisible`       | `:invisible`                                          |
| `:book`            | can be removed, mdoc uses book mode by default        |
| `:evaluated`       | n/a                                                   |
| `:passthrough`     | `:passthrough`                                        |
| `:decorate(param)` | n/a                                                   |
| `:reset`           | `:reset`                                              |

## Migration script

The following script can be used to translate the most basic and mechanical
differences between tut and mdoc. It is normal that mdoc reports compile errors
after running the migration script and you need some manual fixing to get the
documents compiling.

```scala mdoc:file:bin/migrate-tut.sh

```

## sbt-tut

Use the sbt-mdoc plugin instead of sbt-tut and run `sbt docs/mdoc` instead of
`sbt docs/tut`.

```diff
// project/plugins.sbt
- addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.10")
+ addSbtPlugin("org.scalameta" % "sbt-mdoc" % "@VERSION@")
// build.sbt
- enablePlugins(TutPlugin)
+ enablePlugins(MdocPlugin)
```

The sbt-mdoc plugin exposes only one task `mdoc`.

| tut                    | mdoc                                     |
| ---------------------- | ---------------------------------------- |
| `tut`                  | `mdoc`                                   |
| `tutQuick`             | `mdoc --watch`                           |
| `tutOnly path/doc.md`  | `mdoc --include doc.md`                  |
| `tutSourceDirectory`   | `mdocIn`                                 |
| `tutTargetDirectory`   | `mdocOut`                                |
| `tutNameFilter`        | `mdoc --include <glob> --exclude <glob>` |
| `scalacOptions in Tut` | `scalacOptions in Compile`               |
| `tutPluginJars`        | `addCompilerPlugin()`                    |

Note that mdoc does not use the Scala REPL so compiler options like
`-Ywarn-unused` work normally.

## `:fail`

The tut `:fail` modifier is split in two separate mdoc modifiers.

- `:fail`: for compile errors, mdoc uses a custom compilation mode for these
  code fences.
- `:crash`: for runtime errors, mdoc instruments these code fences like normal
  except they're wrapped in `try/catch` blocks.

## Double binding

It's not possible to bind the same variable twice in mdoc without `:reset`. In
tut, the following program is valid.

````md
```tut
val x = 1
val x = 2
```
````

In mdoc, that program does not compile because the same variable name can't be
redefined.

````scala mdoc:mdoc:crash
```scala mdoc
val x = 1
```
```scala mdoc
val x = 2
```
````

One possible workaround is to use a separate variable name.

````diff
 ```scala mdoc
-val x = 2
+val y = 2
 ```
````

Another possible workaround is to use the `:reset` modifier.

````diff
-```scala mdoc
+```scala mdoc:reset
 val x = 2
 ```
````

However, note that `:reset` discards all previous imports and declarations in
the document.
