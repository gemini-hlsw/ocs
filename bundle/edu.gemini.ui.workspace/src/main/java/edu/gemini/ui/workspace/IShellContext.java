package edu.gemini.ui.workspace;


public interface IShellContext {

	void setTitle(String name);
	
	void addView(IViewAdvisor advisor, String id, IViewAdvisor.Relation rel, String other);	

	IActionManager getActionManager();
	
	IShell getShell();
	
	IWorkspace getWorkspace();
	
}

