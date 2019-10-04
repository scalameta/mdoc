package tests.markdown

import mdoc.PostModifier
import mdoc.PostModifierContext

class PostModifierSuite extends BaseMarkdownSuite {
  check(
    "basic",
    """
      |```scala mdoc:bullet
      |val x = 2
      |```
    """.stripMargin,
    """|1. Bullet
       |2. Bullet
    """.stripMargin
  )

  check(
    "evilplot",
    """
      |```scala mdoc:evilplot:scatter.png
      |import com.cibo.evilplot._
      |import com.cibo.evilplot.plot._
      |import com.cibo.evilplot.plot.aesthetics.DefaultTheme._
      |import com.cibo.evilplot.numeric.Point
      |
      |val data = Seq.tabulate(100) { i =>
      |  Point(i.toDouble,
      |    scala.util.Random.nextDouble())
      |}
      |
      |ScatterPlot(data)
      |  .xAxis()
      |  .yAxis()
      |  .frame()
      |  .xLabel("x")
      |  .yLabel("y")
      |  .render()
      |```
    """.stripMargin,
    """|![](scatter.png)
    """.stripMargin
  )

  checkError(
    "error",
    """
      |```scala mdoc:bullet
      |val x = "message"
      |```
    """.stripMargin,
    "error: expected int runtime value. Obtained message"
  )

}
