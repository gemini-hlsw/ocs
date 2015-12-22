package edu.gemini.ui.workspace.impl;

import java.awt.Component;
import java.awt.Container;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor.Relation;
import edu.gemini.ui.workspace.util.CleanSplitPane;
import edu.gemini.ui.workspace.util.CleanTabbedPane;

/**
 * Manages the visual hierarchy of views.
 * @author rnorris
 */
public class ViewManager {

    private final Container root;
    private final Map<String, View> views = new TreeMap<>();
    private final IShell shell;

    public ViewManager(Container parent, Shell shell) {
        this.root = parent;
        this.shell = shell;
    }

    public View getView(String id) {
        View ret = views.get(id);
        if (ret == null)
            throw new NoSuchElementException(id);
        return ret;
    }

    public void addView(View newView, Relation relation, View oldView) {

        // We can only do this if the "other" view is also managed by us.
        if (oldView != null && !views.containsValue(oldView))
            throw new IllegalArgumentException("Not a managed view: " + newView);

        // Add the new view to our index.
        views.put(newView.getId(), newView);

        // If this is the first view, add it to the root. We're done.
        if (views.size() == 1) {
            root.add(newView.getPeer());
            return;
        }

        // If other is null or other is the one and only view, add at the top
        // level.
        if (oldView == null || getParent(oldView) == root) {
            Component otherPeer = root.getComponent(0);
            root.remove(0);

            switch (relation) {

            case NorthOf:
            case SouthOf:
            case EastOf:
            case WestOf:
                JSplitPane jsp = createSplitPane(newView.getPeer(), relation, otherPeer);
                root.add(jsp);
                break;

            case Above:
            case Beneath:
                JTabbedPane jtp = createTabbedPane(newView.getPeer(), relation, otherPeer);
                root.add(jtp);
                break;

            default:
                throw new IllegalArgumentException();

            }

            return;
        }

        // Ok, we have handled the special cases above. At this point we know
        // that the other view is in one side of a split pane or is a JTabbedPane.
        JComponent oldViewParent = (JComponent) getParent(oldView);
        if (oldViewParent instanceof JSplitPane) {
            JSplitPane oldViewSplitPaneParent = (JSplitPane) getParent(oldView);

            switch (relation) {
            case NorthOf:
            case SouthOf:
            case EastOf:
            case WestOf:


                if (oldViewSplitPaneParent.getLeftComponent() == oldView.getPeer()) {
                    JSplitPane newPane = createSplitPane(newView.getPeer(), relation, oldView.getPeer());
                    oldViewSplitPaneParent.setLeftComponent(newPane);
                } else {
                    JSplitPane newPane = createSplitPane(newView.getPeer(), relation, oldView.getPeer());
                    oldViewSplitPaneParent.setRightComponent(newPane);
                }
                break;

            case Above:
            case Beneath:

                oldViewSplitPaneParent.remove(oldView.getPeer());
                JTabbedPane jtp = createTabbedPane(newView.getPeer(), relation, oldView.getPeer());
                oldViewSplitPaneParent.add(jtp);
                break;

            default:
                throw new IllegalArgumentException();

            }

        } else if (oldViewParent instanceof JTabbedPane) {

            JTabbedPane jtp = (JTabbedPane) oldViewParent;
            jtp.add(newView.getPeer());

        }
    }

    public void removeView(View view) {

        views.remove(view.getId());

        Container parent = getParent(view);

        // If this is the one and only view, just remove it.
        if (parent == root) {
            root.remove(view.getPeer());
            return;
        }

        // Otherwise this view is part of a split pane set.
        JSplitPane pane = (JSplitPane) parent;
        Component otherSide;
        if (pane.getLeftComponent() == view.getPeer()) {
            otherSide = pane.getRightComponent();
        } else {
            otherSide = pane.getLeftComponent();
        }

        // Ok, now replace the split pane with the other peer.
        Container paneParent = pane.getParent();
        if (paneParent == root) {
            root.remove(pane);
            root.add(otherSide);
            return;
        }

        JSplitPane panePane = (JSplitPane) paneParent;
        if (panePane.getLeftComponent() == pane) {
            panePane.setLeftComponent(otherSide);
        } else {
            panePane.setRightComponent(otherSide);
        }


    }

    public Container getParent(View view) {
        return view.getPeer().getParent();
    }

    private JSplitPane createSplitPane(Component comp, Relation rel, Component other) {
        switch (rel) {
        case EastOf:	return new CleanSplitPane(JSplitPane.HORIZONTAL_SPLIT, other, comp);
        case WestOf:	return new CleanSplitPane(JSplitPane.HORIZONTAL_SPLIT, comp, other);
        case SouthOf:	return new CleanSplitPane(JSplitPane.VERTICAL_SPLIT, other, comp);
        case NorthOf:	return new CleanSplitPane(JSplitPane.VERTICAL_SPLIT, comp, other);
        default:
            throw new IllegalArgumentException();
        }
    }

    private JTabbedPane createTabbedPane(Component comp, Relation rel, Component other) {
        JTabbedPane ret = new CleanTabbedPane((Shell) shell, JTabbedPane.BOTTOM);
        switch (rel) {
        case Above:   ret.add(comp); ret.add(other); break;
        case Beneath: ret.add(other); ret.add(comp); break;
        default:
            throw new IllegalArgumentException();
        }
        return ret;
    }

    public void selectView(String id) {
        View v = getView(id);
        Container c = getParent(v);
        if (c instanceof JTabbedPane) {
            ((JTabbedPane) c).setSelectedComponent(v.getPeer());
        }
        v.getAdvisor().setFocus();
    }

}


