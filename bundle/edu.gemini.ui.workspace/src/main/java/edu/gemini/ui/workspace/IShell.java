package edu.gemini.ui.workspace;

import java.awt.datatransfer.Clipboard;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;

import edu.gemini.ui.gface.GSelectionBroker;

@SuppressWarnings("unchecked")
public interface IShell extends GSelectionBroker {

	String PROP_VIEW = "view";
	String PROP_MODEL = "model";
	
	void addPropertyChangeListener(PropertyChangeListener pcl);
	void removePropertyChangeListener(PropertyChangeListener pcl);
	
	void close();
	
	Object getModel();
	void setModel(Object model);
	
	public JFrame getPeer();
	
	void selectView(String string);

	IShellAdvisor getAdvisor();
	
	IShellContext getContext();
	
	/**
	 * Returns the clipboard shared by all shells in this workspace.
	 * @return the shared Clipboard
	 */
	Clipboard getWorkspaceClipboard();
	
}

