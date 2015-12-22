package edu.gemini.ui.workspace.impl;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

import edu.gemini.ui.gface.GSelectionBroker;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.SimpleInternalFrame;

@SuppressWarnings("unchecked")
public class View implements IViewContext {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final Shell shell;
    private final IViewAdvisor advisor;
    private GSelectionBroker<?> selectionBroker;

    private final String id;

    private final SimpleInternalFrame peer;

    final Map<Object, Action> retargetActions = new HashMap<>();

    public View(Shell shell, IViewAdvisor advisor, String id) {
        peer = new SimpleInternalFrame("Untitled");
        this.advisor = advisor;
        this.shell = shell;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setFocused(boolean focused) {
        peer.setSelected(focused);
    }

    public void setContent(Component content) {
        peer.setContent(content);
    }

    public SimpleInternalFrame getPeer() {
        return peer;
    }

    public IViewAdvisor getAdvisor() {
        return advisor;
    }

    public IShell getShell() {
        return shell;
    }

    public void setTitle(String title) {
        peer.setTitle(title);
        peer.setName(title);
    }

    public String getTitle() {
        return peer.getTitle();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        pcs.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        pcs.removePropertyChangeListener(pcl);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + peer.getTitle() + "]";
    }

    public GSelectionBroker getSelectionBroker() {
        return selectionBroker;
    }

    public void setSelectionBroker(GSelectionBroker selectionBroker) {
        this.selectionBroker = selectionBroker;
        shell.getSelectionHub().add(selectionBroker);

    }

    public void setSelection(Object[] selection) {
        new Exception("Ignoring setSelection()").printStackTrace();
    }

    public void addRetargetAction(Object id, Action action) {
        retargetActions.put(id, action);
    }

}




