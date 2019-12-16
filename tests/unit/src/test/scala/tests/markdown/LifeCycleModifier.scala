package tests.markdown

import mdoc._

/**
  * Global counter used to test the [[mdoc.Main]] process counting.
  * Because tests can be executed concurrently, these need to be
  * thread local. The following counters are used for testing within
  * the same thread.
  */
object LifeCycleCounter {
  val numberOfStarts: ThreadLocal[Integer] = ThreadLocal.withInitial(() => 0)
  val numberOfExists: ThreadLocal[Integer] = ThreadLocal.withInitial(() => 0)
}

class LifeCycleModifier extends PostModifier {
  val name = "lifecycle"

  // Starts and stops per instance
  var numberOfStarts = 0
  var numberOfExists = 0

  def process(ctx: PostModifierContext): String = {
    // Used for checking the counting between threads
    s"numberOfStarts = $numberOfStarts ; numberOfExists = $numberOfExists"
  }

  /**
    * This is called once when the [[mdoc.Main]] process starts
    * @param settings CLI or API settings used by mdoc
    */
  override def onStart(settings: MainSettings): Unit = {
    numberOfStarts += 1
    LifeCycleCounter.numberOfStarts.set(LifeCycleCounter.numberOfStarts.get() + 1)
  }

  /**
    * This is called once when the [[mdoc.Main]] process finishes
    * @param exit is the exit code returned by mdoc's processing
    */
  override def onExit(exit: Int): Unit = {
    numberOfExists += 1
    LifeCycleCounter.numberOfExists.set(LifeCycleCounter.numberOfExists.get() + 1)
  }
}
