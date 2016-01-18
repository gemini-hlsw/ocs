package edu.gemini.ui.workspace;

import edu.gemini.ui.workspace.util.InternalFrameHelp;

import java.util.Optional;

public interface IShellContext {

    void setTitle(String name);

    void addView(IViewAdvisor advisor, String id, IViewAdvisor.Relation rel, String other);

    void addView(IViewAdvisor advisor, String id, IViewAdvisor.Relation rel, String other, Optional<InternalFrameHelp> helpButton);

    IActionManager getActionManager();

    IShell getShell();

    IWorkspace getWorkspace();

}

