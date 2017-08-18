package jsky.app.ot.tpe;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.stream.Collectors;

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

        public TpeFeatureData(TpeImageFeature feature, JToggleButton button) {
            this.feature = feature;
            this.button = button;
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

        // Load the desired value from the preferences or set to the default.
        // Do this before adding the item listener so that the feature isn't
        // added to the image widget as a side-effect.  Features are initialized
        // in the image widget by updateAvailableOptions.
        btn.setSelected(Preferences.get(prefName, tif.isEnabledByDefault()));

        btn.addItemListener(e -> {
            final boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            if (selected) {
                _iw.addFeature(tif);
            } else {
                _iw.deleteFeature(tif);
            }
            Preferences.set(prefName, selected);
        });

        _featureMap.put(name, new TpeFeatureData(tif, btn));

        _tpeToolBar.addViewItem(btn, tif.getCategory());
    }

    public void updateAvailableOptions(final Collection<TpeImageFeature> feats, final TpeContext ctx) {

        // TpeImageFeatures are clearly meant to each have a unique name as
        // nothing in this class would work otherwise.  Here we remember those
        // that are supposed to be available.
        final Set<String> available = feats.stream().filter(tif -> tif.isEnabled(ctx)).map(TpeImageFeature::getName).collect(Collectors.toSet());

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
        }
    }
}
