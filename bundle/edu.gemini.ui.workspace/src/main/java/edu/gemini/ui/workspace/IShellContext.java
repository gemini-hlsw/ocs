package edu.gemini.ui.workspace;

import javax.swing.*;

public interface IShellContext {

    void setTitle(String name);

    void addView(IViewAdvisor advisor, String id, IViewAdvisor.Relation rel, String other);

    void addView(IViewAdvisor advisor, String id, IViewAdvisor.Relation rel, String other, Action helpAction, Icon helpIcon );

    IActionManager getActionManager();

    IShell getShell();

    IWorkspace getWorkspace();

}

