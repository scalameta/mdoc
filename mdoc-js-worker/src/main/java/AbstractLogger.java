package mdoc.js.worker;

import org.scalajs.logging.Logger;

/* The worker compiles against the Scala 2 build of scalajs-logging. If a Scala class implements
 * Logger, the class calls the static method Logger.$init$. The Scala 3 build of scalajs-logging
 * does not have that method. javac does not add that call. A Scala class that extends this class
 * does not implement Logger itself, so it does not call Logger.$init$ either. */
abstract class AbstractLogger implements Logger {}
