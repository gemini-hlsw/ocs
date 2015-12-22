package edu.gemini.ui.workspace;

import java.awt.Component;

import javax.swing.Action;

import edu.gemini.ui.gface.GSelectionBroker;

public interface IViewContext {

    IShell getShell();
    void setTitle(String title);
    String getTitle();

    void setContent(Component comp);

    @SuppressWarnings("unchecked")
    void setSelectionBroker(GSelectionBroker selectionBroker);

    @Deprecated
    void setSelection(Object[] selection);

    void addRetargetAction(Object id, Action action);

}
