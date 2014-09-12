package edu.gemini.ui.workspace.scala

import edu.gemini.ui.workspace._

trait RichShellAdvisor[A] extends IShellAdvisor {

  final def open(context: IShellContext) = open(enrichShellContext[A](context))
  final def close(context: IShellContext) = close(enrichShellContext[A](context))

  def open(context: RichShellContext[A])
  def close(context: RichShellContext[A]): Boolean

}