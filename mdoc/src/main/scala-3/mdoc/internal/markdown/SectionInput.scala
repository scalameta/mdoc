// package mdoc.internal.markdown

// import scala.meta.inputs.Position

// import dotty.tools.dotc.core.Contexts.Context
// import dotty.tools.dotc.parsing.Parsers.Parser
// import dotty.tools.dotc.util.SourceFile
// import dotty.tools.dotc.util.ScriptSourceFile

// import mdoc.internal.cli.{Context => MContext}
// import dotty.tools.dotc.ast.untpd._
// import scala.meta.Source
// import scala.meta.inputs.Input
// import scala.meta.Name
// import mdoc.internal.pos.TokenEditDistance
// import mdoc.internal.BuildInfo
// import dotty.tools.dotc.interactive.InteractiveDriver

// /* The class uses Scala 3 parser.
//  * Can be removed once the project is updated to use scalameta parser for Scala 3*/
// case class SectionInput(input : Input, mod : Modifier, context : MContext){

//   private val driver = new InteractiveDriver(List("-color:never", "-classpath", context.settings.classpath))
//   private val wrapIdent = " " * 2
//   private val sourceCode =
//       s"""|object OUTER{
//           |$wrapIdent${new String(input.chars).replace("\n", "\n"+ wrapIdent)}
//           |}
//           |""".stripMargin
//   private val filename = "Section.scala"
//   driver.run(java.net.URI.create("file:///Section.scala"), SourceFile.virtual(filename, sourceCode))
//   val source = driver.currentCtx.run.units.head.untpdTree
//   val ctx = driver.currentCtx
//   def stats : List[Tree] = {
//       source match {
//         case PackageDef(_, List(module @ _ : ModuleDef)) => 
//           module.impl.body(using driver.currentCtx)
//         case _ => Nil
//       }
//   }

//   def show(tree : Tree, currentIdent : Int) = {
//      val str = tree.sourcePos(using ctx).start 
//      val end = tree.sourcePos(using ctx).end
//      val realIdent = " " * (currentIdent - wrapIdent.size)
//      sourceCode.substring(str, end).replace("\n", "\n" + realIdent)
//   }
//   def text = source.show(using driver.currentCtx)
// }

// object SectionInput { 
//   // note(@tgodzik) Needed since pack the code into an object when parsing
//   // alternative would be to use ScriptSourceFile, but that currently ignores toplevel expressions
//   val startLine = 1
//   val startIdent = 2
  
//   def tokenEdit(sections: List[SectionInput], instrumented: Input): TokenEditDistance = {
//     TokenEditDistance.fromInputs(sections.map(_.input), instrumented)
//   }

//   def apply(input : Input, context : MContext): SectionInput = {
//     SectionInput(input, Modifier.Default(), context)
//   }
// }
