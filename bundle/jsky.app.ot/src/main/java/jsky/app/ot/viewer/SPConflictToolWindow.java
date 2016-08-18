package jsky.app.ot.viewer;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.spModel.util.ReadableNodeName;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.template.EdTemplateGroup;
import jsky.app.ot.util.OtColor;
import jsky.util.gui.Resources;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Side panel that appears whenever a node has conflicts to resolve.
 * Offers information, controls, and a way to mark the conflict resolved.
 */
public final class SPConflictToolWindow extends JPanel {
    private static final Logger LOG = Logger.getLogger(SPConflictToolWindow.class.getName());

    /**
     * Defines an interface that can be used by the controls in this panel to
     * dismiss the conflict panel when the conflict has been resolved.
     * Conflicts are resolved by removing the node's
     * {@link edu.gemini.pot.sp.ISPNode#CONFLICTS_KEY} client
     * data, which doesn't generate any events.  Accordingly, the container of
     * the conflict tool window, the SPViewer, doesn't know when the conflict
     * has been resolved and must be explicitly told via an implementation of
     * this interface.
     */
    public static interface Dismiss {
        void apply();
    }

    private final JPanel content = new JPanel(new GridBagLayout()) {{
        // initialize ?  anything to do here?
    }};

    private ISPNode node;
    private final Dismiss dismiss;
    private final SPViewer viewer;
    private final SPTree tree;

    SPConflictToolWindow(final Dismiss dismiss, SPViewer viewer, SPTree tree) {
        super(new GridBagLayout());
        this.dismiss = dismiss;
        this.viewer  = viewer;
        this.tree    = tree;
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        add(content, new GridBagConstraints() {{
            gridy  = 1;
            fill   = BOTH;
            anchor = NORTHWEST;
            weightx = 1.0;
            fill    = HORIZONTAL;
            insets = new Insets(0,0,0,0);
        }});

        // push everything up to the top
        add(new JPanel(), new GridBagConstraints() {{
            gridy   = 4;
            fill    = VERTICAL;
            weighty = 1.0;
        }});

        // not sure this is a good idea.  won't it seem like it will only
        // work on the current node?
//        final JButton resolveButton = new JButton(viewer._actions.resolveConflictsAction);
//        add(resolveButton, new GridBagConstraints() {{
//            gridy   = 5;
//            insets  = new Insets(5,0,0,0);
//        }});

        Dimension d = new Dimension(225, 0);
        setPreferredSize(d);
        setMinimumSize(d);
    }

    // Toggle the data object conflict perspective and update the UI to match.
    private void togglePerspective() {
        if (node != null) {
            node.swapDataObjectConflict();
            update();
        }
    }

    /**
     * Sets the node to edit with this tool window and updates the UI to match.
     */
    void setNode(ISPNode node) {
        this.node = node;
        update();
    }

    // We can't allow people to delete template folders or template groups with
    // observations.
    private boolean canDelete(ISPNode target) {
        if (target instanceof ISPTemplateFolder) return false;
        else if (target instanceof ISPTemplateGroup) {
            return ((ISPTemplateGroup) target).getTemplateParameters().size() == 0;
        } else return true;
    }

    /**
     * Returns <code>true</code> if the node being edited has data object
     * conflicts.
     */
    public boolean hasDataObjectConflict() {
        return (node != null) && !node.getConflicts().dataObjectConflict.isEmpty();
    }

    public boolean hasEditableConflicts() {
        // the conflict folder itself has a conflict to prevent the program
        // from being committed, but we don't want to show the editor in this
        // case
        return (node != null) && node.hasConflicts() && !(node instanceof ISPConflictFolder);
    }

    private final class ConflictWidgets {
        final JEditorPane pane;
        final JPanel controls;

        ConflictWidgets(JEditorPane pane, JPanel controls) {
            if (pane == null) throw new NullPointerException("pane is null");
            if (controls == null) throw new NullPointerException("controls is null");
            this.pane     = pane;
            this.controls = controls;
        }
    }

    private final HyperlinkListener linkListener = new HyperlinkListener() {
        @Override public void hyperlinkUpdate(HyperlinkEvent e) {
            if (HyperlinkEvent.EventType.ACTIVATED.toString().equals(e.getEventType().toString())) {
                final String url = e.getDescription();
                final String key = url.substring(url.indexOf("://") + 3);
                click(new SPNodeKey(key));
            }
        }

        private void click(SPNodeKey key) {
            if (node == null) return;
            final ISPNode toNode = SPTreeUtil.findByKey(node.getProgram(), key);
            if (toNode == null) {
                JOptionPane.showMessageDialog(SPConflictToolWindow.this, "Program Node was deleted.", "Deleted Node", JOptionPane.WARNING_MESSAGE);
            } else if (toNode instanceof ISPTemplateParameters) {
                // Template parameters have no editor so we have to show the
                // template group they are contained in.  Special case to
                // highlight the template parameters in the table :/
                tree.setSelectedNode(toNode.getParent());
                final OtItemEditor ed =  viewer.getCurrentEditor();
                if (ed instanceof EdTemplateGroup) {
                    final EdTemplateGroup edGroup = (EdTemplateGroup) ed;
                    edGroup.selectParameters((ISPTemplateParameters) toNode);
                }
            } else {
                tree.setSelectedNode(toNode);
            }
        }
    };

    private JEditorPane mkPane(final String html) {
        final String wrapper = "<html><style type=\"text/css\">body { font:12pt dialog,sans-serif; }</style><body>%s</body></html>";
        return new JEditorPane("text/html", String.format(wrapper, html)) {{
            setBackground(OtColor.BG_GREY);
            setHighlighter(null);
            setEditable(false);
            addHyperlinkListener(linkListener);
        }};
    }

    private JButton mkOkButton(final String title, final Conflict.Note cn) {
        return new JButton(title, Resources.getIcon("eclipse/checkBox.gif")) {{
            setHorizontalAlignment(LEFT);
            setToolTipText("Acknowledge this change and dismiss it from the listing.");
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    node.resolveConflict(cn);
                    SPConflictToolWindow.this.update();
                }
            });
        }};
    }

    private JPanel wrapOkButton(final Conflict.Note cn) {
        return new JPanel(new GridBagLayout()) {{
            add(mkOkButton("Ok, thanks", cn), new GridBagConstraints());
            add(new JPanel(), new GridBagConstraints() {{
                gridx=1;
                fill=HORIZONTAL;
                weightx=1.0;
            }});
        }};
    }

    // Gets the conflict data object perspective (i.e., is the conflict
    // data object associated with the local database or the remote database.
    // Defaults to remote if there is no conflict.
    private DataObjectConflict.Perspective getPerspective() {
        DataObjectConflict.Perspective res = DataObjectConflict.Perspective.REMOTE;
        if (node != null) {
            res = node.getConflicts().dataObjectConflict.map(new MapOp<DataObjectConflict, DataObjectConflict.Perspective>() {
                @Override public DataObjectConflict.Perspective apply(DataObjectConflict doc) {
                    return doc.perspective;

                }
            }).getOrElse(DataObjectConflict.Perspective.REMOTE);
        }
        return res;
    }

    // Formats the perspective into a display value to present.
    private static String toDisplay(DataObjectConflict.Perspective p) {
        switch (p) {
            case LOCAL: return "local version";
            default: return "remote version";
        }
    }

    private ConflictWidgets dataObjectConflict() {
        final DataObjectConflict.Perspective p = getPerspective();
        final String html =
            "Your changes conflict with the version in the remote database. " +
            "Use the \"Display\" button below to switch between local and remote "+
            "versions. You may make additional changes in the editor to the left. " +
            "When satisfied with the displayed version, click the \"Keep\" " +
            "button to acknowledge that it is the one you want to keep.";

        final JButton toggleButton = new JButton(String.format("Display %s", toDisplay(p)), Resources.getIcon("eclipse/cycle.gif")) {{
            setHorizontalAlignment(SwingConstants.LEFT);
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { togglePerspective(); }
            });
            setToolTipText("Click to toggle between the local and remote versions of this item.");
        }};
        final JButton confirmButton = new JButton("Keep displayed version", Resources.getIcon("eclipse/checkBox.gif")) {{
            setToolTipText("Acknowledge this change and dismiss it from the listing.");
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    node.resolveDataObjectConflict();
                    SPConflictToolWindow.this.update();
                }
            });
        }};
        final JPanel controls = new JPanel(new GridBagLayout()) {{
            add(toggleButton, new GridBagConstraints() {{
                gridx     = 0;
                gridy     = 0;
                fill      = HORIZONTAL;
                anchor    = WEST;
                insets    = new Insets(5,0,0,0);
            }});
            add(confirmButton, new GridBagConstraints() {{
                gridx     = 0;
                gridy     = 1;
                fill      = HORIZONTAL;
                anchor    = WEST;
                insets    = new Insets(5,0,0,0);
            }});
        }};

        return new ConflictWidgets(mkPane(html), controls);
    }

    private static final String link(ISPNode node) {
        final String title = ReadableNodeName.format(node);
        final String shortTitle = (title.length() > 35) ? String.format("%s ...", title.substring(0, 30)) : title;
        return String.format("<a href=\"sp://%s\">%s</a>", node.getNodeKey(), shortTitle);
    }

    private class NoteUi implements Conflict.NoteVisitor {
        private final ISPNode root;
        private final ISPNode cNode;

        // Mutable members
        JPanel controls;
        String html;

        NoteUi(SPNodeKey conflictNodeKey) {
            this.root  = node.getProgram();
            this.cNode = SPTreeUtil.findByKey(root, conflictNodeKey);
        }

        private final String name(SPNodeKey key) {
            final ISPNode root  = node.getProgram();
            final ISPNode child = SPTreeUtil.findByKey(root, key);
            return (child == null) ? "" : ReadableNodeName.format(child);
        }

        private void redoDelete(final Conflict.Note cn) {
            if (!(node instanceof ISPContainerNode)) return;

            final java.util.List<ISPNode> children = ((ISPContainerNode) node).getChildren();

            boolean deleted = false;
            final ListIterator<ISPNode> lit = children.listIterator();
            while (lit.hasNext()) {
                ISPNode n = lit.next();
                if (n.getNodeKey().equals(cn.getNodeKey())) {
                    lit.remove();
                    deleted = true;
                    break;
                }
            }

            if (deleted) {
                try {
                    ((ISPContainerNode) node).setChildren(children);
                } catch (SPException ex) {
                    LOG.log(Level.WARNING, "Shouldn't get an SPException removing a child node", ex);
                }
                node.resolveConflict(cn);
                update();
            }
        }

        private JPanel deleteControls(final Conflict.Note cn) {
            final JButton delete = new JButton("No, delete it again", Resources.getIcon("eclipse/remove.gif")) {{
                addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) { redoDelete(cn); }
                });
                setToolTipText("Delete node and resolve conflict.");
                setHorizontalAlignment(LEFT);
            }};

            final JPanel controls = new JPanel(new GridBagLayout()) {{
                add(mkOkButton("Ok, let's keep it", cn), new GridBagConstraints() {{
                    gridy  = 0;
                    insets = new Insets(3,0,0,0);
                    anchor = WEST;
                    fill   = HORIZONTAL;
                }});
                add(delete, new GridBagConstraints() {{
                    gridy  = 1;
                    insets = new Insets(5,0,0,0);
                    anchor = WEST;
                    fill   = HORIZONTAL;
                }});
                add(new JPanel(), new GridBagConstraints() {{
                    gridx      = 1;
                    gridheight = 2;
                    fill       = HORIZONTAL;
                    weightx    = 1.0;
                }});
            }};
            return controls;
        }


        @Override
        public void visitMoved(Conflict.Moved note) {
            ISPNode toParent = SPTreeUtil.findByKey(root, note.getDestinationKey());
            String toLink = (toParent == null) ? "another node" : link(toParent);
            html = (cNode != null) ?
                    String.format("%s was moved to %s", link(cNode), toLink) :
                    "A child node was moved in the remote database but has been subsequently deleted";
        }

        @Override
        public void visitResurrectedLocalDelete(Conflict.ResurrectedLocalDelete note) {
            if (cNode != null) {
                html = String.format("You had deleted %s but someone else stored a change to it that you haven't seen. It has been replaced so you can examine it.", link(cNode));
                controls = canDelete(cNode) ? deleteControls(note) : null;
            } else {
                html = "A locally deleted child node was replaced but then subsequently deleted again";
            }
        }

        @Override
        public void visitReplacedRemoteDelete(Conflict.ReplacedRemoteDelete note) {
            if (cNode != null) {
                html = String.format("Someone else deleted %s but since you had made unstored changes, it has been replaced so you can decide whether to keep it.", link(cNode));
                controls = canDelete(cNode) ? deleteControls(note) : null;
            } else {
                html = "Kept modified child node but it was subsequently deleted";
            }
        }

        @Override
        public void visitCreatePermissionFail(Conflict.CreatePermissionFail note) {
            html = "Reset status to Phase II.";
        }

        @Override
        public void visitUpdatePermissionFail(Conflict.UpdatePermissionFail note) {
            if (cNode != null) {
                html = String.format("This is an editable copy of %s, which contained changes that could not be automatically merged with your updates.", link(cNode));
            } else {
                html = "This is a copy of an observation that has been deleted";
            }
        }

        @Override
        public void visitDeletePermissionFail(Conflict.DeletePermissionFail note) {
            html = "After merging changes made by other users, you no longer have permission to delete this observation.";
        }

        @Override
        public void visitConstraintViolation(Conflict.ConstraintViolation note) {
            html = "After merging changes made by other users, this node is no longer legal in this context.";
        }

        @Override
        public void visitConflictFolder(Conflict.ConflictFolder note) {
            html = "";
        }
    }

    private ConflictWidgets conflictNote(final Conflict.Note cn) {
        final NoteUi ui = new NoteUi(cn.getNodeKey());
        cn.accept(ui);
        final JPanel controls = ui.controls;
        final String html     = ui.html;

        return new ConflictWidgets(mkPane(html), controls == null ? wrapOkButton(cn) : controls);
    }

    private void update() {
        if ((node == null) || !node.hasConflicts() || (node instanceof ISPConflictFolder)) {
            dismiss.apply();
            return;
        }

        final java.util.List<ConflictWidgets> widgetsList = new ArrayList<ConflictWidgets>();

        if (hasDataObjectConflict()) widgetsList.add(dataObjectConflict());
        node.getConflicts().notes.foreach(new ApplyOp<Conflict.Note>() {
            @Override public void apply(Conflict.Note cn) {
                widgetsList.add(conflictNote(cn));
            }
        });

        content.removeAll();

        int row = 0;
        for (ConflictWidgets cw : widgetsList) {
            final int y    = row;
            final int vGap = (row == 0) ? 0 : 10;

            content.add(cw.pane, new GridBagConstraints() {{
                gridy   = y;
                weightx = 1.0;
                fill    = HORIZONTAL;
                anchor  = WEST;
                insets  = new Insets(vGap,0,0,0);
            }});

            content.add(cw.controls, new GridBagConstraints() {{
                gridy   = y+1;
                weightx = 1.0;
                fill    = HORIZONTAL;
                anchor  = WEST;
                insets  = new Insets(5,0,0,0);
            }});
            row = row + 2;
        }

        // TODO: which of this mess is needed to make the changes visible?
        content.validate();
        content.revalidate();
        content.repaint();
    }
}
