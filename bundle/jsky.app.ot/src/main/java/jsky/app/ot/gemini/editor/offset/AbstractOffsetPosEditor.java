package jsky.app.ot.gemini.editor.offset;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.guide.DefaultGuideOptions;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeMap;
import edu.gemini.spModel.target.*;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListWatcher;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import edu.gemini.spModel.telescope.IssPort;
import jsky.app.ot.editor.OtItemEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Control code for the {@link OffsetPosUI}.  Handles the guide star links,
 * leaves the position coordinates to the subclass.
 */
public abstract class AbstractOffsetPosEditor<P extends OffsetPosBase> {
    private static final Logger LOG = Logger.getLogger(AbstractOffsetPosEditor.class.getName());

    protected interface PosValueSetter<P extends OffsetPosBase, V> {
        void apply(P pos, V value);
    }

    protected final PosValueSetter<P, Double> pSetter = new PosValueSetter<P, Double>() {
        @Override public void apply(P pos, Double value) { pos.setXAxis(value); }
    };

    protected final PosValueSetter<P, Double> qSetter = new PosValueSetter<P, Double>() {
        @Override public void apply(P pos, Double value) { pos.setYAxis(value); }
    };

    protected class DoubleTextWidgetListener extends AbstractDocumentListener {
        private final PosValueSetter<P, Double> setter;

        public DoubleTextWidgetListener(PosValueSetter<P, Double> setter) {
            this.setter = setter;
        }

        public void textChanged(DocumentEvent docEvent, String newText) {
            double newVal = 0.0;
            try {
                newVal = Double.valueOf(newText);
            } catch (Exception ex) {
                // ignore
            }

            try {
                for (P pos : getAllSelectedPos()) {
                    pos.deleteWatcher(posWatcher);
                    setter.apply(pos, newVal);
                    pos.addWatcher(posWatcher);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private static final Object NO_GUIDE_OPTION = new Object() {
        public String toString() {
            return "";
        }
    };

    protected abstract class ComboBoxAction<G extends GuideOption> implements ActionListener {
        private final JComboBox combo;

        protected ComboBoxAction(JComboBox combo) {
            this.combo = combo;
        }

        public final void actionPerformed(ActionEvent e) {
            final Object val = combo.getSelectedItem();
            if (val == NO_GUIDE_OPTION) return;

            final G opt = (G) combo.getSelectedItem();
            for (P pos : getAllSelectedPos()) {
                pos.deleteWatcher(posWatcher);
                updatePos(opt, pos);
                pos.addWatcher(posWatcher);
            }
        }

        protected abstract void updatePos(G opt, P pos);
    }

    protected final class DefaultComboAction extends ComboBoxAction<DefaultGuideOptions.Value> {
        public DefaultComboAction(JComboBox combo) { super(combo); }
        protected final void updatePos(DefaultGuideOptions.Value opt, P pos) {
            pos.setDefaultGuideOption(opt);
        }
    }

    protected final class AdvancedComboAction extends ComboBoxAction<GuideOption> {
        private final GuideProbe guider;
        public AdvancedComboAction(GuideProbe guider, JComboBox combo) {
            super(combo);
            this.guider = guider;
        }
        protected final void updatePos(GuideOption opt, P pos) {
            pos.setLink(guider, opt);
        }
    }

    public class TextFieldActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final OffsetPosList<P> opl = getPositionList();
            if (opl == null) return;

            final List<P> selList = getAllSelectedPos();
            if ((selList == null) || (selList.size() == 0)) return;

            final P lastSelPos = selList.get(selList.size()-1);
            final int index = opl.getPositionIndex(lastSelPos) + 1;
            if (index >= opl.size()) {
//                if (editor != null) editor.apply();
                return;
            }

            final P selPos = opl.getPositionAt(index);
            select(selPos);

//            if (editor != null) editor.apply();

            final JTextField tf = (JTextField) e.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tf.selectAll();
                }
            });
        }
    }

    private OtItemEditor<? extends ISPNode, ? extends IOffsetPosListProvider<P>> editor;
    private OffsetPosList<P> opl;
    private boolean editable;
    private final OffsetPosUI ui;

    private IssPort port = IssPort.DEFAULT;

    private final DefaultComboAction defaultComboAction;
    private final Map<GuideProbe, AdvancedComboAction> comboBoxListeners;

    private List<P> watchedPositions = new ArrayList<P>();
    protected TelescopePosWatcher posWatcher = new TelescopePosWatcher() {
        public void telescopePosUpdate(WatchablePos tp) {
            if (opl != null) selectionUpdated(getAllSelectedPos());
        }
    };

    public AbstractOffsetPosEditor(OffsetPosUI ui) {
        this.ui = ui;

        defaultComboAction = new DefaultComboAction(ui.getDefaultGuiderCombo());
        Map<GuideProbe, AdvancedComboAction> m = new HashMap<GuideProbe, AdvancedComboAction>();
        for (GuideProbe guider : GuideProbeMap.instance.values()) {
            m.put(guider, new AdvancedComboAction(guider, ui.getAdvancedGuiderCombo(guider)));
        }
        comboBoxListeners = Collections.unmodifiableMap(m);

        addWfsComboBoxListeners();
    }

    private void addWfsComboBoxListeners() {
        ui.getDefaultGuiderCombo().addActionListener(defaultComboAction);
        for (GuideProbe guider : GuideProbeMap.instance.values()) {
            ui.getAdvancedGuiderCombo(guider).addActionListener(comboBoxListeners.get(guider));
        }
    }
    private void removeWfsComboBoxListeners() {
        for (GuideProbe guider : GuideProbeMap.instance.values()) {
            ui.getAdvancedGuiderCombo(guider).removeActionListener(comboBoxListeners.get(guider));
        }
        ui.getDefaultGuiderCombo().removeActionListener(defaultComboAction);
    }

    protected void selectionUpdated(List<P> selList) {
        removePosWatchers();
        setPosWatchers(selList);
        updateWfsCombos(selList);
    }

    protected final void removePosWatchers() {
        // Remove any existing position watchers.
        for (P pos : watchedPositions) pos.deleteWatcher(posWatcher);
    }

    protected final void setPosWatchers(List<P> selList) {
        watchedPositions = selList;
        for (P pos : watchedPositions) pos.addWatcher(posWatcher);
    }

    private final PropertyChangeListener offsetSelWatcher = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            selectionUpdated(getAllSelectedPos());
        }
    };

    private final OffsetPosListWatcher<P> posListWatcher = new OffsetPosListWatcher<P>() {
        @Override public void posListReset(OffsetPosList<P> tpl) {}
        @Override public void posListAddedPosition(OffsetPosList<P> tpl, List<P> newPos) {}
        @Override public void posListRemovedPosition(OffsetPosList<P> tpl, List<P> rmPos) {}

        @Override
        public void posListPropertyUpdated(OffsetPosList<P> tpl, String propertyName, Object oldValue, Object newValue) {
            if (!OffsetPosList.ADVANCED_GUIDING_PROP.equals(propertyName)) return;
            ui.setGuiders(opl.getAdvancedGuiding());
            updateWfsCombos(getAllSelectedPos());
        }
    };

    protected OffsetPosUI getEditorUI() {
        return ui;
    }

    protected OffsetPosList<P> getPositionList() {
        return opl;
    }

    protected List<P> getAllSelectedPos() {
        return OffsetPosSelection.apply(editor.getNode()).selectedPositions(opl);
    }

    protected void select(P pos) {
        OffsetPosSelection.select(opl, pos).commit(editor.getNode());
    }

    public void setPositionList(OtItemEditor<? extends ISPNode, ? extends IOffsetPosListProvider<P>> editor, boolean editable) {
        OffsetPosSelection.deafTo(editor.getNode(), offsetSelWatcher);

        this.editor   = editor;
        this.editable = editable;
        if (opl != null) {
            opl.deleteWatcher(posListWatcher);
        }
        opl = editor.getDataObject().getPosList();
        ui.setGuiders(opl.getAdvancedGuiding());
        opl.addWatcher(posListWatcher);
        OffsetPosSelection.listenTo(editor.getNode(), offsetSelWatcher);
        selectionUpdated(getAllSelectedPos());
    }

    protected void syncGuideState(Set<GuideProbe> available, Set<GuideProbe> noPrimary) {
        updateWfsCombos(getAllSelectedPos());
    }

    protected IssPort getIssPort() {
        return port;
    }

    void setIssPort(IssPort port) {
        this.port = port;
    }

    /**
     * Updates the WFS combo boxes to match the selection.
     */
    protected void updateWfsCombos(List<P> selList) {
        removeWfsComboBoxListeners();

        final Set<GuideProbe> referencedGuiders = opl.getAdvancedGuiding();
        try {
            // Handle an empty selection.
            if ((selList == null) || (selList.size() == 0)) {
                // Set all the values to guide and disable the widgets.
                ui.getDefaultGuiderCombo().setSelectedItem(DefaultGuideOptions.instance.getDefault());
                ui.getDefaultGuiderCombo().setEnabled(false);
                for (GuideProbe guider : referencedGuiders) {
                    JComboBox cb = ui.getAdvancedGuiderCombo(guider);
                    cb.setSelectedItem(guider.getGuideOptions().getDefault());
                    cb.setEnabled(false);
                }
                return;
            }

            // Okay, something is selected.  Enable the combo boxes for the
            // referenced guiders, unless they have no primary guide star
            ui.getDefaultGuiderCombo().setEnabled(editable);
            for (GuideProbe guider : referencedGuiders) {
                JComboBox cb = ui.getAdvancedGuiderCombo(guider);
                // Enable combo boxes for guide probes that are available in
                // the observation and have a primary guide star selected.
//                boolean enabled = availableGuiders.contains(guider) && !noPrimaryGuiders.contains(guider)
//                        && OT.isEditable();
                cb.setEnabled(editable);
            }


            // Remember the GuideOptions for the first position.
            final P firstPos = selList.get(0);
            DefaultGuideOptions.Value defGuideOption = firstPos.getDefaultGuideOption();
            final Map<GuideProbe, GuideOption> guideMap;
            guideMap = new HashMap<GuideProbe, GuideOption>();
            for (final GuideProbe guider : referencedGuiders) {
                guideMap.put(guider, firstPos.getLink(guider, guider.getGuideOptions().getDefault()));
            }

            // Now look at the remaining selected targets and determine whether
            // they agree with the first position.
            for (final P pos : selList.subList(1, selList.size())) {
                DefaultGuideOptions.Value opt = pos.getDefaultGuideOption();
                if (opt != defGuideOption) {
                    defGuideOption = null;
                    break; // no need to examine remaining pos
                }
            }
            nextGuider: for (final GuideProbe guider : referencedGuiders) {
                for (final P pos : selList.subList(1, selList.size())) {
                    GuideOption opt = pos.getLink(guider, guider.getGuideOptions().getDefault());
                    if (opt != guideMap.get(guider)) {
                        // The selected positions don't agree on the value for
                        // this wfs option.  So, show nothing.
                        guideMap.put(guider, null);
                        continue nextGuider; // no need to examine remaining pos
                    }
                }
            }

            // Update the combo boxes to show the correct
                // have to go to the model directly since setting a value not
                // in the model (e.g., NO_GUIDER) doesn't work through the
                // widget itself
            ui.getDefaultGuiderCombo().getModel().setSelectedItem(defGuideOption == null ? NO_GUIDE_OPTION : defGuideOption);
            for (GuideProbe guider : referencedGuiders) {
                JComboBox cb = ui.getAdvancedGuiderCombo(guider);
                GuideOption opt = guideMap.get(guider);
                cb.getModel().setSelectedItem(opt == null ? NO_GUIDE_OPTION : opt);
            }
        } finally {
            addWfsComboBoxListeners();
        }
    }
}
