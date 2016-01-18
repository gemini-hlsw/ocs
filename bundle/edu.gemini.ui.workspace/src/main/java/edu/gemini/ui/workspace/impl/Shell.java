package edu.gemini.ui.workspace.impl;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.Clipboard;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GSelectionBroker;
import edu.gemini.ui.gface.util.SelectionHub;
import edu.gemini.ui.workspace.IActionManager;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IShellAdvisor;
import edu.gemini.ui.workspace.IShellContext;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewAdvisor.Relation;
import edu.gemini.ui.workspace.IWorkspace;
import edu.gemini.ui.workspace.util.RetargetAction;
import edu.gemini.ui.workspace.util.SimpleInternalFrame;

@SuppressWarnings("unchecked")
public class Shell implements IShell, PropertyChangeListener, WindowFocusListener {

    private static final Logger LOGGER = Logger.getLogger(Shell.class.getName());
    private static final KeyboardFocusManager FOCUS_MANAGER = KeyboardFocusManager.getCurrentKeyboardFocusManager();

    private final Set<View> views = new HashSet<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final IShellAdvisor advisor;
    private final JFrame peer = new ShellFrame();
    private final ActionManager menuManager = new ActionManager(peer.getJMenuBar());
    private final ViewManager viewManager = new ViewManager(peer.getContentPane(), this);
    private final ShellContext context = new ShellContext();
    private final SelectionHub hub = new SelectionHub();
    private final Workspace workspace;

    private Object model;
    private View focusView;
    private boolean closed = false;
    private Component focusWait;

    Shell(Workspace workspace, IShellAdvisor advisor) {
        FOCUS_MANAGER.addPropertyChangeListener("permanentFocusOwner", this);
        this.workspace = workspace;
        this.advisor = advisor;
        peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        peer.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                close();
            }
        });
        peer.addWindowFocusListener(this);


        hub.addPropertyChangeListener(GSelectionBroker.PROP_SELECTION, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                pcs.firePropertyChange(evt);
            }

        });

    }

    public IShellAdvisor getAdvisor() {
        return advisor;
    }

    public synchronized void open() {
        // Should this be synchronous?
        SwingUtilities.invokeLater(() -> {
            advisor.open(context);
            peer.setLocationByPlatform(true);
            peer.pack();
            peer.setVisible(true);
        });
    }

    public synchronized void close() {
        if (!closed) {
            if (closed = advisor.close(context)) {
                peer.setVisible(false);
                peer.dispose();
                LOGGER.fine("Window closed.");
            } else {
                LOGGER.info("Window close was cancelled.");
            }
        } else {
            LOGGER.fine("(Window was already closed).");
        }
    }

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        Object prev = this.model;
        this.model = model;
        pcs.firePropertyChange(PROP_MODEL, prev, model);
    }

    public JFrame getPeer() {
        return peer;
    }

    public void windowLostFocus(WindowEvent we) {
        if (focusView != null)
            focusView.setFocused(false);
    }

    public void windowGainedFocus(WindowEvent arg0) {
        if (focusView != null)
            focusView.setFocused(true);
    }

    private synchronized void setFocus(View newFocusView) {
        if (newFocusView != focusView) {
            for (View v: views) {
                if (v != newFocusView) {
                    v.setFocused(false);
                } else {

                    for (RetargetAction ra: menuManager.retargetActions)
                        ra.setTarget(newFocusView == null ? null : newFocusView.retargetActions.get(ra.getId()));

                    invokeAndWait(new FocusTask(newFocusView));
                }
            }
        }
    }

    public IShellContext getContext() {
        return context;
    }

    private static void invokeAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while setting focus.", e);
            } catch (InvocationTargetException e) {
                LOGGER.log(Level.WARNING, "Trouble setting focus.", e.getTargetException());
            }
        }
    }


    public void propertyChange(PropertyChangeEvent pce) {

        if (pce.getSource() == FOCUS_MANAGER) {
            if (FOCUS_MANAGER.getFocusedWindow() == peer) {
                Component c = FOCUS_MANAGER.getPermanentFocusOwner();

                // You can tell the Shell to wait for a particular component (or its
                // parent) to become focused, in order to deal with focus bounce when
                // switching tabs on a JTabbedPane. The CleanTabbedPane does this.
                if (focusWait != null) {
                    Component w = c;
                    while (w != focusWait && w != null)
                        w = w.getParent();
                    if (w != focusWait) {
                        LOGGER.fine("Ignoring bounce.");
                        return;
                    }
                    LOGGER.fine("Bounce avoided. Continuing with focus.");
                    focusWait = null;
                }

                while (c != null && !(c instanceof SimpleInternalFrame))
                    c = c.getParent();
                for (View v: views) {
                    if (v.getPeer() == c) {
                        LOGGER.fine("Setting focus to " + v);
                        setFocus(v);
                    }
                }
            }
        }

//		} else if ((pce.getSource() == focusView || pce.getSource() == this) && pce.getPropertyName().equals(View.PROP_SELECTION)) {

            // The selection for the focused view has changed. Note that unlike
            // in the change of focus case, we DO want to forward assertions of
            // a null selection. This means that the current view has gone from
            // some defined selection to a state where it makes no sense to talk
            // about a selection.

            // Note that the "don't fire if the value hasn't changed" logic
            // doesn't work with arrays so we're doing it here.
//			if (!Arrays.equals((Object[]) pce.getOldValue(), (Object[]) pce.getNewValue()))
//				pcs.firePropertyChange(PROP_SELECTION, pce.getOldValue(), pce.getNewValue());

//		}
    }

    private class FocusTask implements Runnable {

        private final View newFocusView;

        private FocusTask(View newFocusView) {
            this.newFocusView = newFocusView;
        }

        public void run() {
            final View prev = focusView;

            // This causes focus to bounce, so we need to do it synchronously
            // on the swing thread. That's why this thing is in a runnable.
//			newFocusView.getAdvisor().setFocus();
            newFocusView.setFocused(true);
            focusView = newFocusView;

            pcs.firePropertyChange(PROP_VIEW, prev, focusView);

            // If the new view has a selection broker, set it as primary.
            GSelectionBroker<?> broker = focusView == null ? null : focusView.getSelectionBroker();
            if (broker != null)
                hub.setPrimaryBroker(broker);


//			// Update the shell's selection if necessary. The shell's selection
//			// only changes if the selected view makes a positive assertion that
//			// it has a selection to provide. Null means that there is no
//			// selection, whereas a 0-length array means that the selection is
//			// empty.
//			if (focusView != null) {
//				Object[] newSelection = newFocusView == null ? null : newFocusView.getSelection();
//				if (newSelection != null) {
//					Object[] oldSelection = prev == null ? null : prev.getSelection();
//					
//					// Note that the "don't fire if the value hasn't changed" logic
//					// doesn't work with arrays so we're doing it here.
//					if (!Arrays.equals(oldSelection, newSelection))
//						pcs.firePropertyChange(PROP_SELECTION, oldSelection, newSelection);
//				}
//			}

        }

    }

    private class ShellContext implements IShellContext {

        public void setTitle(String name) {
            peer.setTitle(name);
        }

        @Override
        public void addView(final IViewAdvisor advisor, String id, Relation rel, String otherId) {
            View other = (otherId == null) ? null : viewManager.getView(otherId);
            final View view = new View(Shell.this, advisor, id);
            views.add(view);
            view.addPropertyChangeListener(Shell.this);
            advisor.open(view);

            viewManager.addView(view, rel, other);
        }

        @Override
        public void addView(final IViewAdvisor advisor, String id, Relation rel, String otherId, Action helpAction, Icon helpIcon, String hint) {
            View other = (otherId == null) ? null : viewManager.getView(otherId);
            final View view = new View(Shell.this, advisor, id, helpAction, helpIcon, hint);
            views.add(view);
            view.addPropertyChangeListener(Shell.this);
            advisor.open(view);

            viewManager.addView(view, rel, other);
        }

        public IActionManager getActionManager() {
            return menuManager;
        }

        public IShell getShell() {
            return Shell.this;
        }

        public IWorkspace getWorkspace() {
            return workspace;
        }

    }

    public void selectView(String id) {
        viewManager.selectView(id);
    }

    SelectionHub getSelectionHub() {
        return hub;
    }

    public GSelection<?> getSelection() {
        return hub.getSelection();
    }

    public void setSelection(GSelection newSelection) {
        GSelection<?> prev = getSelection();
        hub.setSelection(newSelection);
        pcs.firePropertyChange(PROP_SELECTION, prev, newSelection); // RCN: hmm
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    public void waitForFocus(Component component) {
        this.focusWait = component;
    }

    public Clipboard getWorkspaceClipboard() {
        return workspace.getClipboard();
    }

}
