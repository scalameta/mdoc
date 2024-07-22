---
id: js
title: Scala.js Modifiers
sidebar_label: Scala.js
---

Code fences with the `scala mdoc:js` modifier are compiled with Scala.js and run
in the browser.

```scala mdoc:js:shared:invisible
import org.scalajs.dom.window._
import jsdocs._
```

````md
```scala mdoc:js
val progress = new Progress()
setInterval({ () =>
  // `node` variable is a DOM element in scope.
  node.innerHTML = progress.tick(5)
}, 100)
```
````

```scala mdoc:js:invisible
Loading progress bar...
---
val progress = new Progress()
setInterval({ () =>
  // `node` variable is a DOM element in scope.
  node.innerHTML = progress.tick(5)
}, 100)
```


## Installation

The `mdoc:js` modifier requires custom installation steps.

### sbt-mdoc

First, install sbt-mdoc using the
[regular installation instructions](installation.md#sbt).

Next, update the `mdocJS` setting to point to a Scala.js project that has
`scalajs-dom` as a library dependency.

```diff
// build.sbt
lazy val jsdocs = project
  .settings(
+   libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "@SCALAJS_DOM_VERSION@"
  )
  .enablePlugins(ScalaJSPlugin)
lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
+   mdocJS := Some(jsdocs)
  )
  .enablePlugins(MdocPlugin)
```

### Command-line

First, install [Coursier](https://get-coursier.io/docs/cli-overview).

Next, construct an `mdoc.properties` file with the necessary Scala.js
configuration

```sh
cat <<EOT > mdoc.properties
js-classpath=$(coursier fetch org.scala-js:scalajs-library_@SCALA_BINARY_VERSION@:@SCALAJS_VERSION@ org.scala-js:scalajs-dom_sjs@SCALAJS_BINARY_VERSION@_@SCALA_BINARY_VERSION@:@SCALAJS_DOM_VERSION@ -p)
js-scalac-options=-Xplugin:$(coursier fetch --intransitive org.scala-js:scalajs-compiler_@SCALA_VERSION@:@SCALAJS_VERSION@)
js-linker-classpath=$(coursier fetch org.scalameta:mdoc-js-worker_@SCALA_BINARY_VERSION@:@VERSION@ org.scala-js:scalajs-linker_@SCALA_BINARY_VERSION@:@SCALAJS_VERSION@ -p)
EOT
```

Note: For scala 3, you may need `js-scalac-options=-scalajs` - instead of the XPlugin incantation - [Doc](https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html#compiler-plugins).



Next, create a basic Markdown files with an `mdoc:js` modifier:

```sh
mkdir docs
cat <<EOT > docs/index.md
\`\`\`scala mdoc:js
org.scalajs.dom.window.setInterval(() => {
  node.innerHTML = new java.util.Date().toString
}, 1000)
\`\`\`
EOT
```

Next, launch mdoc with the `org.scalameta:mdoc-js` module instead of
`org.scalameta:mdoc`:

```sh
coursier launch org.scalameta:mdoc-js_@SCALA_BINARY_VERSION@:@VERSION@ --extra-jars $(pwd) -- --watch
```

Open the URL http://localhost:4001/index.md to see a live preview of the
generated Markdown.

Some notes:

- The `--extra-jars $(pwd)` flag is needed to pick up the `mdoc.properties`
  configuration.
- The `js-scalacOptions` field in `mdoc.properties` must contain
  `-Xplugin:path/to/scalajs-compiler.jar` to enable the Scala.js compiler.
- The `js-classpath` field in `mdoc.properties` must include a dependency on the
  library `org.scala-js:scalajs-dom`

## Modifiers

The following modifiers can be combined with `mdoc:js` code fences to customize
the rendered output.

### `:shared`

By default, each code fence is isolated from other code fences. Use the
`:shared` modifier to reuse imports or variables between code fences.

```scala mdoc:js:shared:invisible
import org.scalajs.dom.window.setInterval
import scala.scalajs.js.Date
```

````scala mdoc:mdoc
```scala mdoc:js:shared
import org.scalajs.dom.window.setInterval
import scala.scalajs.js.Date
```
```scala mdoc:js
setInterval(() => {
  node.innerHTML = new Date().toString()
}, 1000)
```
````

```scala mdoc:js
Loading <code>:shared</code> example...
---
setInterval(() => {
  val date = new Date().toString()
  node.innerHTML = s"<p>Shared date $date</p>"
}, 1000)
```

Without `:shared`, the example above results in a compile error.

````scala mdoc:mdoc:crash
```scala mdoc:js
import scala.scalajs.js.Date
```
```scala mdoc:js
new Date()
```
````

### `:invisible`

By default, the original input code is rendered in the output page. Use
`:invisible` to hide the code example from the output so that only the div is
generated.

````scala mdoc:mdoc
```scala mdoc:js:invisible
var n = 0
org.scalajs.dom.window.setInterval(() => {
  n += 1
  node.innerHTML = s"Invisible tick: $n"
}, 1000)
```
````

```scala mdoc:js:invisible
Loading <code>:invisible</code> example...
---
var n = 0
setInterval(() => {
  n += 1
  node.innerHTML = s"Invisible tick: ${n}"
}, 1000)
```

### `:compile-only`

Use `:compile-only` to validate that a code example compiles successfully
without evaluating it at runtime.

````md
```scala mdoc:js:compile-only
org.scalajs.dom.window.setInterval(() => {
  println("I'm only compiled, never executed")
}, 1000)
```
````

Note that `compile-only` blocks do not automatically have access to a `node` DOM
element. Create a leading code fence with `shared:invisible` to expose a hidden
`node` instance in the scope of a `compile-only` code block.

## Loading HTML

By default, the `node` variable points to an empty div element. Prefix the code
fence with custom HTML followed by a `---` separator to set the inner HTML of
the `node` div.

````scala mdoc:mdoc
```scala mdoc:js
<p>I am a custom <code>loader</code></p>
---
println(node.innerHTML)
```
````

```scala mdoc:js
<p>I am a custom <code>loader</code></p>
---
// Open developer console to see this printed message
println(s"Loading HTML: ${node.innerHTML}")
```

Replace the node's `innerHTML` to make the HTML disappear once the document has
loaded.

```scala mdoc:js
I disappear in 3 seconds...
---
org.scalajs.dom.window.setTimeout(() => {
 node.innerHTML = "I am loaded. Refresh the page to load me again."
}, 3000)
```

## Using scalajs-bundler

The [scalajs-bundler](https://scalacenter.github.io/scalajs-bundler/) plugin can
be used to install npm dependencies and bundle applications with webpack.

Add the following sbt settings if you use scalajs-bundler.

```diff
 lazy val jsdocs = project
   .settings(
+    webpackBundlingMode := BundlingMode.LibraryOnly()
+    scalaJSUseMainModuleInitializer := true,
     npmDependencies.in(Compile) ++= List(
       // ...
     ),
   )
   .enablePlugins(ScalaJSBundlerPlugin)

 lazy val docs = project
   .settings(
     mdocJS := Some(jsdocs),
+    mdocJSLibraries := webpack.in(jsdocs, Compile, fullOptJS).value
   )
   .enablePlugins(MdocPlugin)
```

> The `webpackBundlingMode` must be `LibraryOnly` so that the mdoc generated
> output can depend on it.

### Bundle npm dependencies

It's important that the main function in `jsdocs` uses the installed npm
dependencies. If the npm dependencies are not used from the `jsdocs` main
function, then webpack thinks they are unused and removes them from the bundled
output even if those dependencies are called from `mdoc:js` markdown code
fences.

For example, to use the npm [`ms` package](https://www.npmjs.com/package/ms)
start by writing a facade using `@JSImport`

```scala mdoc:file:tests/jsdocs/src/main/scala/jsdocs/ms.scala

```

Next, write a main function that uses the facade. Make sure that the `jsdocs`
project contains the setting `scalaJSUseMainModuleInitializer := true`.

```scala mdoc:file:tests/jsdocs/src/main/scala/jsdocs/Main.scala

```

The `ms` function can now be used from `mdoc:js`.

```scala mdoc:js
val date = new scala.scalajs.js.Date()
val time = jsdocs.ms(date.getTime())
node.innerHTML = s"Hello from npm package 'ms': $time"
```

If the `ms` function is not referenced from the `jsdocs` main function you get a
stacktrace in the browser console like this:

```js
Uncaught TypeError: $i_ms is not a function
    at $c_Lmdocjs$.run6__Lorg_scalajs_dom_raw_Element__V (js.md.js:1180)
    at js.md.js:3108
    at mdoc.js:9
```

### Validate library.js and loader.js

Validate that the `webpack` task provides one `*-library.js` file and one
`*-loader.js` file.

```scala
// sbt shell
> show jsdocs/fullOptJS::webpack
...
[info] * Attributed(.../jsdocs/target/scala-2.12/scalajs-bundler/main/jsdocs-opt-loader.js)
[info] * Attributed(.../jsdocs/target/scala-2.12/scalajs-bundler/main/jsdocs-opt-library.js)
...
```

These files are required by mdoc in order to use the scalajs-bundler npm
dependencies. The files may be missing if you have custom webpack or
scalajs-bundler configuration. To fix this problem, you may want to try to
create a new `jsdocs` Scala.js project with minimal webpack and scalajs-bundler
configuration.

## Configuration

The `mdoc:js` modifier supports several site variable options to customize the
rendered output of `mdoc:js`.

### Add custom HTML header

Update the `js-html-header` site variable to insert custom HTML before the
compiled JavaScript. For example, to add React via unpkg add the following
setting.

```diff
 mdocVariables := Map(
+  "js-html-header" ->
+     """<script crossorigin src="https://unpkg.com/react@16.5.1/umd/react.production.min.js"></script>"""
 )
```

### Generate optimized page

The Scala.js `fullOpt` mode is used by default and the `fastOpt` mode is used
when the `-w` or `--watch` flag is used. The `fastOpt` mode provides faster
feedback while iterating on documentation at the cost of larger bundle size and
slower code. When publishing a website, the optimized mode should be used.

Update the `js-opt` site variables to override the default optimization mode:

- `js-opt=full`: use `fullOpt` regardless of watch mode
- `js-opt=fast`: use `fastOpt` regardless of watch mode

### Customize `node` variable name

By default, each `mdoc:js` code fence has access to a `node` variable that
points to a DOM element.

Update the site variable `js-mount-node=customNode` to use a different variable
name than `node`.

### Customize output js directory

By default, mdoc generates the javascript next to the output markdown sources.
For example, a `foo.md` markdown file with `mdoc:js` code fences produces a
`foo.md.js` JavaScript file in the same directory.

When using the `DocusaurusPlugin` sbt plugin, the output directory is
automatically configured to emit JavaScript in the `assets/` directory since the
`docs/` directory can only contain markdown sources.

For other site generators than Docusaurus or outside of sbt, update the site
variable `js-out-prefix=assets` if you need to generate the output JavaScript
file in a different directory than the markdown source.

### Customize module kind

By default, sbt-mdoc uses the `scalaJSModuleKind` sbt setting to determine the
module kind.

Outside of sbt, update the `js-module-kind` site variables to customize the
module kind:

- `js-module-kind=NoModule`: don't export modules, the default value.
- `js-module-kind=CommonJSModule`: use CommonJS modules.
- `js-module-kind=ESModule`: use ECMAScript modules, not supported.

### Add local JavaScript libraries

In sbt, the `mdocJSLibraries` setting allows you to link local `*-library.js`
and `*-loader.js` files produced by webpack via scalajs-bundler.

Outside of sbt, update the `js-libraries` site variable to contain a path
separated list of local JavaScript files (same syntax as Java classpaths) to
link local JavaScript library files. Note that mdoc will only copy the following
files:

- `*-library.js`: this file is copied to the output directory and linked from
  the markdown output as a `<script src="...">`.
- `*-loader.js`: same as `*-library.js` but it comes after `*-library.js`.
- `*.js.map`: optional source maps.

### Batch mode linking

By default, mdoc will re-use the Scala.js linker, exercising its ability to work
incrementally. This can speed up linking and optimization dramatically.

If incremental linking is causing issues in your project, use can use `js-batch-mode`
site variable to enable batch mode (which will discard intermediate linker state
after processing each file):

```scala
mdocVariables := Map(
  "js-batch-mode" -> "true"
)
```
### Remapping imports at link time.

It may be the case, that the library you wish to depend on, is a facade to some NPM dependancy. Setting up a bundler in mdoc isn't a simple operation, but there is an alternative. We can instead as mdoc to output ESModules (see `ModuleKind`) and then remap their imports to point at a CDN.

To mdocs CLI, you'll need to pass this flag;

```sh
--import-map-path full/path/to/import-map.json
```

`import-map.json` should look like this;

```json
{
  "imports": {
    "@easepick/bundle": "https://cdn.jsdelivr.net/npm/@easepick/bundle@1.2.1",
  }
}
```
You should then have this dependancy available to you.
