package edu.gemini.ui.workspace;

import java.awt.Component;

import javax.swing.Action;

import edu.gemini.ui.gface.GSelectionBroker;

public interface IViewContext {

//	String ACTION_MENU_PATH = IViewContext.class.getName() + "/action.menu.path";
//	String ACTION_TOOLBAR_PATH = IViewContext.class.getName() + "/action.toolbar.path";
	
	IShell getShell();
//	void setSelection(Object[] selection);
	void setTitle(String title);
	String getTitle();
	
	void setContent(Component comp);
	Component getContent();
	
	@SuppressWarnings("unchecked")
	void setSelectionBroker(GSelectionBroker selectionBroker);
//	GSelectionBroker getSelectionBroker();	
	
	@Deprecated
	void setSelection(Object[] selection);
	
//	void setActions(Collection<Action> actions);
	
	void addRetargetAction(Object id, Action action);
	
}
