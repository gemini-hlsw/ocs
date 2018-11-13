package edu.gemini.qpt.ui.view.variant;

import static edu.gemini.qpt.ui.util.SharedIcons.ADD;
import static edu.gemini.qpt.ui.util.SharedIcons.ADD_DISABLED;
import static edu.gemini.qpt.ui.util.SharedIcons.ARROW_DOWN;
import static edu.gemini.qpt.ui.util.SharedIcons.ARROW_DOWN_DISABLED;
import static edu.gemini.qpt.ui.util.SharedIcons.ARROW_UP;
import static edu.gemini.qpt.ui.util.SharedIcons.ARROW_UP_DISABLED;
import static edu.gemini.qpt.ui.util.SharedIcons.DUP_VARIANT;
import static edu.gemini.qpt.ui.util.SharedIcons.DUP_VARIANT_DIS;
import static edu.gemini.qpt.ui.util.SharedIcons.REMOVE;
import static edu.gemini.qpt.ui.util.SharedIcons.REMOVE_DISABLED;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.ui.CommonActions;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.qpt.ui.util.SimpleToolbar;
import edu.gemini.qpt.ui.util.SimpleToolbar.IconButton;
import edu.gemini.qpt.ui.util.SimpleToolbar.StaticText;
import edu.gemini.qpt.ui.view.variant.edit.DeleteAction;
import edu.gemini.qpt.ui.view.variant.edit.PasteAction;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GSelectionBroker;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

public class VariantViewAdvisor implements IViewAdvisor, PropertyChangeListener, ActionListener {

    private static final Logger LOGGER = Logger.getLogger(VariantViewAdvisor.class.getName());

    private IViewContext context;

    // This viewer is the main thing here.
    private GTableViewer<Schedule, Variant, VariantAttribute> viewer  =
        new GTableViewer<Schedule, Variant, VariantAttribute>(new VariantController());

    // UI Stuff
    private final JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTable());
    private final JButton add = new IconButton(ADD, ADD_DISABLED);
    private final JButton del = new IconButton(REMOVE, REMOVE_DISABLED);
    private final JButton dup = new IconButton(DUP_VARIANT, DUP_VARIANT_DIS);
    private final JButton up = new IconButton(ARROW_UP, ARROW_UP_DISABLED);
    private final JButton down = new IconButton(ARROW_DOWN, ARROW_DOWN_DISABLED);
    private final JLabel text = new StaticText("Double-click a variant to edit it.");
    private final JPanel toolbar = new SimpleToolbar();
    private final JPanel content = new JPanel(new BorderLayout());

    // All buttons in one place
    private final JButton[] buttons = { add, del, dup, up, down };

    public VariantViewAdvisor() {

        // Set up the viewer.
        viewer.setColumns(VariantAttribute.Name, VariantAttribute.IQ, VariantAttribute.CC, VariantAttribute.WV, VariantAttribute.Wind, VariantAttribute.LGS);
        viewer.addPropertyChangeListener(GViewer.PROP_SELECTION, this);
        viewer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        viewer.setColumnSize(VariantAttribute.IQ, 25);
        viewer.setColumnSize(VariantAttribute.CC, 25);
        viewer.setColumnSize(VariantAttribute.WV, 25);
        viewer.setColumnSize(VariantAttribute.Wind, 60);
        viewer.setColumnSize(VariantAttribute.LGS, 25);
        viewer.getTable().addMouseListener(doubleClickToEditListener);
        viewer.setDecorator(new VariantDecorator());

        // Set the viewport size for the scrollpane
        ScrollPanes.setViewportHeight(scroll, 11);

        // Set up the toolbar.
        for (JButton b: buttons) {
            b.setEnabled(false);
            b.addActionListener(this);
            toolbar.add(b);
        }
        toolbar.add(text);

        // Put it all together.
        content.add(scroll, BorderLayout.CENTER);
        content.add(toolbar, BorderLayout.SOUTH);

    }

    @SuppressWarnings("serial")
    public void open(IViewContext viewContext) {

        context = viewContext;
        context.setTitle("Plan Variants");
        context.setContent(content);
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);

        context.addRetargetAction(CommonActions.PASTE, new PasteAction(context.getShell(), viewer, true));
        context.addRetargetAction(CommonActions.DELETE, new DeleteAction(context.getShell(), viewer));

    }

    public void close(IViewContext context) {
        viewer.setModel(null);
    }

    public void setFocus() {
        viewer.getTable().requestFocus();
    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent pce) {

        if (pce.getPropertyName().equals(IShell.PROP_MODEL)) {

            // Set the new model
            Schedule schedule = (Schedule) pce.getNewValue();
            viewer.setModel(schedule);

            // Disable buttons if model is null. Add is enabled if schedule is non-null.
            if (schedule == null) {
                for (JButton b: buttons)
                    b.setEnabled(false);
            } else {
                add.setEnabled(true);
            }

            // This is an odd case where the selection is actually relevant to
            // the model, so we need to push an initial selection down into
            // the view.
            if (schedule != null) {
                Variant v = schedule.getCurrentVariant();
                if (v != null) {
                    viewer.setSelection(new GSelection<Variant>(v));
                }
            }

        }

        if (pce.getPropertyName().equals(GSelectionBroker.PROP_SELECTION)) {

            if (pce.getSource() == viewer) {

                // User clicked on something (somewhere)
                GSelection<Variant> sel = viewer.getSelection();
                Schedule schedule = viewer.getModel();

                // Set the current variant and assert it as our selection.
                if (schedule != null) {
                    schedule.setCurrentVariant(sel.isEmpty() ? null : sel.first());
                    LOGGER.fine("Asserting new selection: " + sel);
                    context.getShell().setSelection(sel);
                }

                // Set button enablement (except for add, which is enabled based on the schedul
                // being null or not -- see the PROP_MODEL handler above).
                if (sel.isEmpty()) {

                    // All disabled.
                    up.setEnabled(false);
                    down.setEnabled(false);
                    dup.setEnabled(false);
                    del.setEnabled(false);

                } else {

                    // Dup and del are enabled.
                    dup.setEnabled(true);
                    del.setEnabled(true);

                    // Up and down depend on the selection AND the position in the list,
                    // so we need to call this method both here AND when the reorder methods
                    // are called (since they change the position but not the selection).
                    setUpDownEnabledState(sel);

                }

            }

        }

    }

    private void setUpDownEnabledState(GSelection<Variant> sel) {

        // Should never be empty
        assert !sel.isEmpty();

        // Up and down depend on whether we have selected the first or last variant.
        // Remember that the viewer is not sorting or filtering any of the variants; we
        // are viewing the full model list in the model-specified order. So we can just
        // look at the model to figure this out.
        final Schedule schedule = viewer.getModel();
        if (schedule == null) {
            up.setEnabled(false);
            down.setEnabled(false);
        } else {
            final Variant v = sel.first();
            final List<Variant> all = schedule.getVariants();
            up.setEnabled(v != all.get(0));
            down.setEnabled(v != all.get(all.size() - 1));
        }

    }

    public void actionPerformed(ActionEvent e) {

        // Collect some info to get started.
        Object source = e.getSource();
        GSelection<Variant> sel = viewer.getSelection();
        Variant v = sel.isEmpty() ? null : sel.first();

        ImOption.apply(viewer.getModel()).foreach(schedule -> {

            // Force a repaint of the action if it's a button. This is an AWT bug.
            if (source instanceof JButton)
                ((JButton) source).repaint();

            if (source == add) {

                // Add a new variant. Selection will not change but position might.
                VariantEditor ve = new VariantEditor(context.getShell().getPeer());
                if (JOptionPane.CANCEL_OPTION != ve.showNew("Untitled", (byte) 0, (byte) 0, (byte) 0))
                    schedule.addVariant(ve.getVariantName(), ve.getVariantConditions(), ve.getVariantWindConstraint(), ve.getVariantLgsConstraint());
                setUpDownEnabledState(sel);

            } else if (source == del) {

                // Delete the selected variant. Selection will change.
                if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(context.getShell().getPeer(),
                        "Do you really want to remove this variant?",
                        "Confirm Remove", JOptionPane.YES_NO_OPTION))
                    return;

                schedule.removeVariant(v);

            } else if (source == up) {

                // Move up. Selection will not change but position will.
                schedule.moveVariant(v, -1);
                setUpDownEnabledState(sel);

            } else if (source == down) {

                // Move down. Selection will not change but position will.
                schedule.moveVariant(v, 1);
                setUpDownEnabledState(sel);

            } else if (source == dup) {

                // Duplicate. Selection will not change but position might.
                Variant dup = schedule.duplicateVariant(v);
                dup.setName("Copy of " + dup.getName());
                setUpDownEnabledState(sel);

            }
        });
    }

    private final MouseListener doubleClickToEditListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                GSelection<Variant> sel = viewer.getSelection();
                if (sel.size() == 1) {
                    VariantEditor ve = new VariantEditor(context.getShell().getPeer());
                    Variant v = sel.first();
                    if (JOptionPane.CANCEL_OPTION != ve.showEditor(v)) {
                        v.setName(ve.getVariantName());
                        v.setConditions(ve.getVariantConditions());
                        v.setWindConstraint(ve.getVariantWindConstraint());
                        v.setLgsConstraint(ve.getVariantLgsConstraint());
                    }
                }
            }
        }
    };

}


