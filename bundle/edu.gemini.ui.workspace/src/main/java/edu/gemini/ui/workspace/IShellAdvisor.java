package edu.gemini.ui.workspace;

public interface IShellAdvisor {

	enum ServiceKey {		
		Id		
	}
	
	void open(IShellContext context);
	boolean close(IShellContext context);
	
}
