package edu.gemini.ui.workspace;

public interface IViewAdvisor {
    enum Relation {
        NorthOf, SouthOf, EastOf, WestOf, Above, Beneath
    }

    void open(IViewContext context);
    void close(IViewContext context);

    void setFocus();
}
