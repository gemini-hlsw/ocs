// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeFeatureManager.java 45662 2012-05-31 02:54:05Z fnussber $
//
package jsky.app.ot.tpe;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 * This is a helper object used to manage the various TpeImageFeatures.
 * This logically belongs with the TelescopePosEditor code but has been
 * moved out to simplify.
 */
final class TpeFeatureManager {

    /**
     * Local class used to map image features to toggle button widgets
     */
    private static class TpeFeatureData {
        final TpeImageFeature feature;  // The image feature itself
        final JToggleButton button;     // The button used to toggle it
        final Option<Component> key;    // Key used to explain the feature

        public TpeFeatureData(TpeImageFeature feature, JToggleButton button, Option<Component> key) {
            this.feature = feature;
            this.button = button;
            this.key = key;
        }
    }

    /**
     * Reference to the position editor toolbar
     */
    private final TpeToolBar _tpeToolBar;

    /**
     * Reference to the image display widget
     */
    private final TpeImageWidget _iw;

    /**
     * Maps image features to toggle button widgets
     */
    private final Map<String, TpeFeatureData> _featureMap = new HashMap<>();


    /**
     * Constructor
     */
    TpeFeatureManager(TelescopePosEditor tpe, TpeImageWidget iw) {
        _tpeToolBar = tpe.getTpeToolBar();
        _iw = iw;
        _tpeToolBar.hideViewButtons();
    }

    /**
     * Add a feature.
     */
    public void addFeature(final TpeImageFeature tif) {
        final String name = tif.getName();

        // Name used to store setting in user preferences.
        final String prefName = tif.getClass().getName() + ".selected";

        // See if this feature is already present.
        if (_featureMap.containsKey(name)) return;

        final JToggleButton btn = new JCheckBox(name);
        btn.setToolTipText(tif.getDescription());
        btn.setVisible(false);
        btn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final boolean selected = e.getStateChange() == ItemEvent.SELECTED;
                if (selected) {
                    _iw.addFeature(tif);
                } else {
                    _iw.deleteFeature(tif);
                }
                Preferences.set(prefName, selected);
            }
        });

        final Option<Component> keyPanel = tif.getKey().isEmpty() ? None.<Component>instance() :
                                             new Some<>(TpeToolBar.createKeyPanel(tif.getKey().getValue()));

        _featureMap.put(name, new TpeFeatureData(tif, btn, keyPanel));

        _tpeToolBar.addViewItem(btn, tif.getCategory());
        if (keyPanel.isDefined()) {
            final Component comp = keyPanel.getValue();
            comp.setVisible(false);
            _tpeToolBar.addViewItem(comp, tif.getCategory());
        }

        // Load the desired value from the preferences or set to the default.
        btn.setSelected(Preferences.get(prefName, tif.isEnabledByDefault()));
    }

    public void updateAvailableOptions(Collection<TpeImageFeature> feats, TpeContext ctx) {

        // TpeImageFeatures are clearly meant to each have a unique name as
        // nothing in this class would work otherwise.  Here we remember those
        // that are supposed to be available.
        final Set<String> available = new HashSet<>();
        for (final TpeImageFeature tif : feats) {
            if (tif.isEnabled(ctx)) {
                available.add(tif.getName());
            }
        }

        // Walk through all the features and make them visible or not as
        // appropriate.
        for (TpeFeatureData data : _featureMap.values()) {
            final boolean visible = available.contains(data.feature.getName());
            setAvailable(data.feature, visible);
            if (visible && isSelected(data.feature)) {
                _iw.addFeature(data.feature);
            } else {
                _iw.deleteFeature(data.feature);
            }
        }
    }

    /**
     * Is the given feature already added?
     */
    public boolean isFeaturePresent(TpeImageFeature tif) {
        return _featureMap.containsKey(tif.getName());
    }

    /**
     * Determines whether the given feature has been selected for viewing in
     * the TPE.
     */
    public boolean isSelected(TpeImageFeature tif) {
        return _featureMap.get(tif.getName()).button.isSelected();
    }

    /**
     * Sets the selected state of the feature, which determines whether it is
     * shown in the TPE.
     */
    public void setSelected(TpeImageFeature tif, boolean selected) {
        TpeFeatureData tfd = _featureMap.get(tif.getName());
        if (selected != tfd.button.isSelected()) {
            tfd.button.doClick();
        }
    }

    /**
     * Determines whether the checkbox to view the given feature is available
     * or not.
     */
    private void setAvailable(TpeImageFeature tif, boolean available) {
        TpeFeatureData tfd = _featureMap.get(tif.getName());
        if (available != tfd.button.isVisible()) {
            tfd.button.setVisible(available);
            if (!tfd.key.isEmpty()) {
                tfd.key.getValue().setVisible(available);
            }
        }
    }
}
