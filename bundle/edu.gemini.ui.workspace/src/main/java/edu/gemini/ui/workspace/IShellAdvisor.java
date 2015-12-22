package edu.gemini.ui.workspace;

public interface IShellAdvisor {
    void open(IShellContext context);
    boolean close(IShellContext context);
}
