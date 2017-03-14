package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import jsky.app.ot.editor.OtItemEditor;

import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


public final class EdCompSiteQuality extends OtItemEditor<ISPObsComponent, SPSiteQuality> {

    private static final String PROP_SITE_QUALITY = "getDataObject()";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final SiteQualityPanel panel = new SiteQualityPanel(this);

    @Override
    public JPanel getWindow() {
        return panel;
    }

    @Override
    public void init() {
        pcs.firePropertyChange(PROP_SITE_QUALITY, null, getDataObject());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    protected void updateEnabledState(boolean enabled) {
        super.updateEnabledState(enabled);
        panel.updateEnabledState(enabled);
    }
}
