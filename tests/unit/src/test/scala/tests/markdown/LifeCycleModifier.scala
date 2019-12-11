package tests.markdown

import java.nio.file.Paths

import mdoc._
import mdoc.internal.cli.{Exit, Settings}

object LifeCycleModifier {
  var numberOfStarts = 0
  var numberOfExists = 0
  var numberOfPreProcess = 0
  var numberOfPostProcess = 0
}

class LifeCycleModifier extends PostModifier {
  val name = "lifecycle"
  //val c = LifeCycleModifier
  var numberOfStarts = 0
  var numberOfExists = 0
  var numberOfPreProcess = 0
  var numberOfPostProcess = 0

  def process(ctx: PostModifierContext): String = {
    val relpath = Paths.get(ctx.info)
    val out = ctx.outputFile.toNIO.getParent.resolve(relpath)
    //val tmp = s"numberOfStarts = ${c.numberOfStarts} ; numberOfExists = ${c.numberOfExists} ; numberOfPreProcess = ${c.numberOfPreProcess} ; numberOfPostProcess = ${c.numberOfPostProcess}"
    println(s"this = $this count @ $LifeCycleModifier")
    val tmp = s"numberOfStarts = ${numberOfStarts} ; numberOfExists = ${numberOfExists} ; numberOfPreProcess = ${numberOfPreProcess} ; numberOfPostProcess = ${numberOfPostProcess}"
    println(tmp)
    tmp
  }

  /*
  override def onStart(settings: Settings): Unit = { c.numberOfStarts += 1 }

  override def preProcess(ctx: PostModifierContext): Unit = { c.numberOfPreProcess += 1}

  override def postProcess(ctx: PostModifierContext): Unit = { c.numberOfPostProcess += 1 }

  override def onExit(exit: Exit): Unit = { c.numberOfExists += 1}
  */

  override def onStart(settings: Settings): Unit = { numberOfStarts += 1 }

  override def preProcess(ctx: PostModifierContext): Unit = { numberOfPreProcess += 1}

  override def postProcess(ctx: PostModifierContext): Unit = { numberOfPostProcess += 1 }

  override def onExit(exit: Exit): Unit = {
    numberOfExists += 1
    println(s"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! $this = $numberOfExists")
  }
}
