package mdoc

import mdoc.internal.markdown.ReplVariablePrinter

/**
  * A captured variable in a code fence.
  *
  * Example, the code fence below has two statements:
  *
  * - The first statement has a single variable with name `x`
  * - The second statement has a two variables with names `y` and `z`
  *
  * {{{
  *   ```scala mdoc
  *   val x = 1
  *   val (y, z) = (2, 3)
  *   ```
  * }}}
  *
  * @param name the variable name, for example `x`
  * @param staticType the pretty-printed static type of this variable, for example `List[Int]`
  * @param runtimeValue the runtime value of this variable.
  * @param indexOfVariableInStatement the index of this variable in the enclosing statement.
  *                                   For example, in `val (a, b) = ???` the variable `a` has index
  *                                   0 and variable `b` has index `1`.
  * @param totalVariablesInStatement The total number of variables in this statements. For example,
  *                                  in `val a = N` the total number is 1 and for `val (a, b) = ...` the
  *                                  total number is 2.
  * @param indexOfStatementInCodeFence the index of the enclosing statement within the enclosing code fence.
  *                                    For example, in
  *                                    {{{
  *                                    ```scala
  *                                    val x = 1
  *                                    val y = 2
  *                                    ```
  *                                    }}}
  *                                    The variable `y` has index 1 and variable `x` has index 0.
  * @param totalStatementsInCodeFence the total number of statement in the enclosing code fence.
  *                                    For example, the total number is 2 for the code fence below.
  *                                    {{{
  *                                    ```scala
  *                                    val x = 1
  *                                    val y = 2
  *                                    ```
  *                                    }}}
  *
  */
final class Variable private[mdoc] (
    val name: String,
    val staticType: String,
    val runtimeValue: Any,
    val indexOfVariableInStatement: Int,
    val totalVariablesInStatement: Int,
    val indexOfStatementInCodeFence: Int,
    val totalStatementsInCodeFence: Int
) {
  def isUnit: Boolean = staticType.endsWith("Unit")
  override def toString: String = {
    ReplVariablePrinter(this)
  }
}
