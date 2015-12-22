package edu.gemini.ui.workspace;

import edu.gemini.ui.workspace.impl.Shell;

public interface IWorkspace {
    void close();

    Shell createShell(IShellAdvisor advisor);
}
