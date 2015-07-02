package jsky.app.ot.tpe;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.util.Resources;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper object to keep up with which tool has been selected and to
 * simplify selecting tools.  This logically belongs with the
 * TelescopePosEditor code but because of its length, its been moved
 * to a separate file.
 */
final class TpeEditorTools {

    static class ButtonState {
        private static final String KEY = ButtonState.class.getName();
        final TpeMode mode;
        final TpeImageFeature feature;
        final TpeCreateableItem item;

        ButtonState(TpeMode mode) {
            this(mode, null, null);
        }

        ButtonState(TpeMode mode, TpeImageFeature feature, TpeCreateableItem item) {
            this.mode    = mode;
            this.feature = feature;
            this.item    = item;
        }

        void set(JToggleButton btn) {
            btn.putClientProperty(KEY, this);
        }

        static ButtonState get(JToggleButton btn) {
            return (ButtonState) btn.getClientProperty(KEY);
        }
    }

    private JToggleButton _current;

    private final TelescopePosEditor _tpe;
    private final TpeToolBar _tpeToolBar;
    private final JToggleButton _browseButton;
    private final JToggleButton _dragButton;
    private final JToggleButton _eraseButton;

    private Map<String, JToggleButton> _createButtonMap = new HashMap<>();

    /** Create with the Presentation that contains the tool buttons. */
    TpeEditorTools(TelescopePosEditor tpe) {
        _tpe = tpe;
        _tpeToolBar = _tpe.getTpeToolBar();

        Icon icon;

        // Browse Tool
        icon = Resources.getIcon("browseArrow.gif");
        _browseButton = new JToggleButton("Browse", icon) {{
            setToolTipText("Switch to browse mode");
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _current = _browseButton;
                    modeChange(TpeMode.BROWSE, None.instance());
                    _tpe.getImageWidget().setCursor(TpeCursor.browse.get());
                }
            });
            setHorizontalAlignment(LEFT);
        }};
        new ButtonState(TpeMode.BROWSE).set(_browseButton);
        _tpeToolBar.addModeButton(_browseButton);

        // Drag Tool
        icon = Resources.getIcon("whiteGlove.gif");
        _dragButton = new JToggleButton("Drag", icon) {{
            setToolTipText("Switch to drag mode");
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _current = _dragButton;
                    modeChange(TpeMode.DRAG, None.instance());
                    _tpe.getImageWidget().setCursor(TpeCursor.drag.get());
                }
            });
            setHorizontalAlignment(LEFT);
        }};
        new ButtonState(TpeMode.DRAG).set(_dragButton);
        _tpeToolBar.addModeButton(_dragButton);

        // Erase Tool
        icon = Resources.getIcon("eclipse/remove.gif");
        _eraseButton = new JToggleButton("Erase", icon) {{
            setToolTipText("Switch to erase mode");
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _current = _eraseButton;
                    modeChange(TpeMode.ERASE, None.instance());
                    _tpe.getImageWidget().setCursor(TpeCursor.erase.get());
                }
            });
            setHorizontalAlignment(LEFT);
        }};
        new ButtonState(TpeMode.ERASE).set(_eraseButton);
        _tpeToolBar.addModeButton(_eraseButton);

        // Hide all the create tools.
        _tpeToolBar.hideCreateButtons();

        // arrange to be notified when the OT editable state changes
        OT.addEditableStateListener(new OT.EditableStateListener() {
            @Override public ISPNode getEditedNode() { return _tpe.getImageWidget().getContext().nodeOrNull(); }
            @Override public void updateEditableState() { updateEnabledStates(); }
        });
    }

    private void modeChange(TpeMode mode, Option<Object> arg) {
        for (TpeImageFeature feat : _tpe.getFeatures()) {
            if (feat instanceof TpeModeSensitive) {
                ((TpeModeSensitive) feat).handleModeChange(mode, arg);
            }
        }
    }

    private boolean isEnabled() {
        final TpeContext ctx = _tpe.getImageWidget().getContext();
        return ctx.progShell().isDefined() && ctx.obsShell().isDefined() && OTOptions.isProgramEditable(ctx.progShell().get()) && OTOptions.isObservationEditable(ctx.obsShell().get());
    }

    /**
     * Update the enable states of the buttons based on the OT editable state.
     */
    public void updateEnabledStates() {
        final boolean enabled = isEnabled();
        _dragButton.setEnabled(enabled);
        _eraseButton.setEnabled(enabled);
    }


    //
    // Add a create tool.
    //
    private void _addCreateTool(TpeCreateableItem item, TpeImageFeature tif) {
        // See if this tool is already present.
        final String label = item.getLabel();
        if (_createButtonMap.get(label) != null) return;

        Icon icon = Resources.getIcon("eclipse/add.gif");
        final JToggleButton btn = new JToggleButton(label, icon) {{
            setToolTipText("Create a new " + label + " position");
            setVisible(false);
            setHorizontalAlignment(LEFT);
        }};
        btn.addActionListener(evt -> {
            _current = btn;

            ButtonState bs = ButtonState.get(btn);
            _tpe.selectFeature(bs.feature);
            _tpe.getImageWidget().setCursor(TpeCursor.add.get());

            modeChange(TpeMode.CREATE, new Some<>(bs.item));
        });

        new ButtonState(TpeMode.CREATE, tif, item).set(btn);

        _tpeToolBar.addCreateButton(btn);

        _createButtonMap.put(label, btn);
    }


    /**
     * Add the create tools for the given feature.
     */
    public void addFeature(TpeImageFeature tif) {
        if (!(tif instanceof TpeCreateableFeature)) return;  // nothing to add
        TpeCreateableItem[] items = ((TpeCreateableFeature) tif).getCreateableItems();
        for (TpeCreateableItem item : items) _addCreateTool(item, tif);
    }

    /**
     * Disable or enable the set of creational tools associated with the
     * given image features.
     */
    public void updateAvailableOptions(Collection<TpeImageFeature> feats) {
        boolean enabled = true;
        if (!isEnabled()) {
            _browseButton.setSelected(true); // make sure we are only in browse mode
            _current = _browseButton;
            enabled = false;
        }

        JToggleButton selected = null;

        // Remove all the existing create buttons.
        for (JToggleButton tbw : _createButtonMap.values()) {
            if (tbw.isSelected()) selected = tbw;
            tbw.setVisible(false);
        }

        // If not enabled, then we're done.
        if (!enabled) return;


        // Add create buttons according to the enabled state of each item.
        for (TpeImageFeature feature : feats) {
            if (!(feature instanceof TpeCreateableFeature)) continue;

            TpeCreateableFeature cFeature = (TpeCreateableFeature) feature;
            for (TpeCreateableItem item : cFeature.getCreateableItems()) {
                if (item.isEnabled(_tpe.getImageWidget().getContext())) {
                    JToggleButton btn = _createButtonMap.get(item.getLabel());
                    btn.setVisible(true);
                }
            }
        }

        if ((selected != null) && !selected.isVisible()){
            _browseButton.doClick();
        }
    }

    /**
     * Get the current mode.  One of MODE_BROWSE, MODE_DRAG, MODE_ERASE, or
     * MODE_CREATE.
     */
    public TpeMode getMode() {
        if (_current == null) return TpeMode.BROWSE;
        return ButtonState.get(_current).mode;
    }

    /**
     * Get the image feature.  This will be null if the mode is not MODE_CREATE.
     * If MODE_CREATE, the image feature will be the one associated with the
     * create button.
     */
    public TpeImageFeature getImageFeature() {
        if (_current == null) return null;
        return ButtonState.get(_current).feature;
    }

    /**
     * Get the creatable item currently selected.
     */
    public TpeCreateableItem getCurrentCreatableItem() {
        if (_current == null) return null;
        return ButtonState.get(_current).item;
    }

    /**
     * Go to browse mode.
     */
    public void gotoBrowseMode() {
        _browseButton.setSelected(true);
    }

    /** Standard debug string. */
    public String toString() {
        return getClass().getName() + "[tool=" + getCurrentCreatableItem() + "]";
    }
}


