package edu.gemini.ui.workspace.scala

import edu.gemini.ui.workspace._

class RichShellContext[A](val context:IShellContext) {
	
  def shell:RichShell[A] = context.getShell
 
  def title:String = sys.error("Not implemented")
  def title_=(s:String) = context.setTitle(s)
  
  def actionManager = context.getActionManager // for now
 
  def workspace = context.getWorkspace()
  
}