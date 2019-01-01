---
id: js
title: Scala.js
---

Use the `mdoc:js` modifier to write dynamic and interactive documentation with
Scala.js.

```scala mdoc:js
Loading...
---
val tick = { () =>
  val date = new scala.scalajs.js.Date()
  val time = s"${date.getHours}h${date.getMinutes}m${date.getSeconds}s"
  node.innerHTML = s"Current time is $time"
}
tick()
org.scalajs.dom.window.setInterval(tick, 1000)
```

Code fences with the `mdoc:js` modifier compile to JavaScript and evaluate at
HTML load time instead of at markdown generation time.

Each `mdoc:js` code fence has access to a variable `node`, which is an empty DOM
element.

## Installation

The `mdoc:js` modifier requires custom installation steps.

### sbt-mdoc

First, install sbt-mdoc using the
[regular installation instructions](installation.md#sbt).

Next, update the `mdocJS` setting to point to a Scala.js project that has
`scalajs-dom` as a library dependency.

```diff
// build.sbt
lazy val jsapp = project
  .settings(
+   libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6"
  )
  .enablePlugins(ScalaJSPlugin)
lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
+   mdocJS := Some(jsapp)
  )
  .enablePlugins(MdocPlugin)
```

### Command-line

First add a dependency on the `org.scalameta:mdoc-js` library.

```diff
 coursier launch \
     org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ \
+    org.scalameta:mdoc-js_@SCALA_BINARY_VERSION@:@VERSION@
```

This dependency enables the `mdoc:js` modifier which requires the site variables
`js-classpath` and `js-scalacOptions`.

```diff
 coursier launch \
     org.scalameta:mdoc_@SCALA_BINARY_VERSION@:@VERSION@ \
     org.scalameta:mdoc-js_@SCALA_BINARY_VERSION@:@VERSION@ -- \
+  --site.js-classpath CLASSPATH_OF_SCALAJS_PROJECT
+  --site.js-scalacOption OPTIONS_OF_SCALAJS_PROJECT
```

- `js-scalacOptions` must contain `-Xplugin:path/to/scalajs-compiler.jar` to
  enable the Scala.js compiler. - `js-classpath` value must include a dependency
  on the library `org.scala-js:scalajs-dom`

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
  node.innerHTML = s"Shared date $date"
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
println(node.innerHTML) // Open developer console to see this printed message
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

## Generate optimized page

The Scala.js `fullOpt` mode is used by default and the `fastOpt` mode is used
when the `-w` or `--watch` flag is used. The `fastOpt` mode provides faster
feedback while iterating on documentation at the cost of larger bundle size and
slower code. When publishing a website, the optimized mode should be used.

Update the `js-opt` site variables to override the default optimization mode:

- `js-opt=full`: use `fullOpt` regardless of watch mode
- `js-opt=fast`: use `fastOpt` regardless of watch mode
