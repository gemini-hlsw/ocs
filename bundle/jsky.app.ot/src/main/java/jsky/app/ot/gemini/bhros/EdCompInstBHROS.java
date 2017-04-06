package jsky.app.ot.gemini.bhros;

import edu.gemini.spModel.gemini.bhros.InstBHROS;
import jsky.app.ot.gemini.editor.EdCompInstBase;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Editor component for bHROS. The hard work is all done in the GUI, so this
 * class is fairly trivial.
 * @author rnorris
 */
public final class EdCompInstBHROS extends EdCompInstBase<InstBHROS> {

	// GUI needs to know when the data object changes, so we support change listeners for this.
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final BhrosForm gui = new BhrosForm(this);

	public JPanel getWindow() {
		return gui;
	}

	@Override protected void init() {
        super.init();
		pcs.firePropertyChange("DataObject", null, getDataObject()); // RCN: ok?
	}

	public TextBoxWidget getPosAngleTextBox() {
		return gui.posAngleTextBox;
	}

	public TextBoxWidget getExposureTimeTextBox() {
		return gui.exposureTimeTextBox;
	}

	public TextBoxWidget getCoaddsTextBox() {
		return gui.coaddsTextBox;
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

}
