package edu.gemini.ui.workspace.scala

import edu.gemini.ui.workspace._

class RichShellContext[A](val context:IShellContext) {
  def shell:RichShell[A] = context.getShell
 
  def title:String = sys.error("Not implemented")
  def title_=(s:String): Unit = context.setTitle(s)
  
  def actionManager: IActionManager = context.getActionManager
 
  def workspace: IWorkspace = context.getWorkspace()
  
}