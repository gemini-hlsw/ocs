// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdCompInstBase.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.TextBoxWidget;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * The base class for all instrument components.  It includes support for
 * a rotation angle text box, something all instruments support.
 */
public abstract class EdCompInstBase<T extends SPInstObsComp> extends OtItemEditor<ISPObsComponent, T>
        implements PropertyChangeListener, jsky.util.gui.TextBoxWidgetWatcher {

    /** If true, ignore property change events */
    private boolean _ignoreChanges = false;

    @Override
    protected void cleanup() {
        if (getDataObject() != null) {
            getDataObject().removePropertyChangeListener(this);
            if (getPosAngleTextBox() != null) getPosAngleTextBox().deleteWatcher(this);
            if (getExposureTimeTextBox() != null) getExposureTimeTextBox().deleteWatcher(this);
            if (getCoaddsTextBox() != null) getCoaddsTextBox().deleteWatcher(this);
        }
    }

    @Override protected void init() {
        if (getDataObject() != null) {
            getDataObject().addPropertyChangeListener(this);

            if (getPosAngleTextBox() != null) {
                getPosAngleTextBox().setText(getDataObject().getPosAngleDegreesStr());
                getPosAngleTextBox().addWatcher(this);
            }

            if (getExposureTimeTextBox() != null) {
                getExposureTimeTextBox().setText(getDataObject().getExposureTimeAsString());
                getExposureTimeTextBox().addWatcher(this);
            }

            if (getCoaddsTextBox() != null) {
                getCoaddsTextBox().setText(getDataObject().getCoaddsAsString());
                getCoaddsTextBox().addWatcher(this);
            }
        }
    }

    // -- Implement the TextBoxWidgetWatcher interface --


    /**
     * A key was pressed in the given TextBoxWidget.
     */
    public void textBoxKeyPress(TextBoxWidget tbwe) {
        if (getDataObject() != null) {
            _ignoreChanges = true;
            try {
                if (tbwe == getPosAngleTextBox()) {
                    /* The default position angle */
                    final double defaultPositionAngle = 0.0;
                    getDataObject().setPosAngleDegrees(tbwe.getDoubleValue(defaultPositionAngle));
                } else if (tbwe == getExposureTimeTextBox()) {
                    double expTime = tbwe.getDoubleValue(getDefaultExposureTime());
                    if (isForceIntegerExposureTime()) expTime = Math.floor(expTime);
                    getDataObject().setExposureTime(expTime);
                } else if (tbwe == getCoaddsTextBox()) {
                    /* The default number of coadds */
                    final int defaultCoadds = 1;
                    getDataObject().setCoadds(tbwe.getIntegerValue(defaultCoadds));
                }
            } finally {
                _ignoreChanges = false;
            }
        }
    }

    // ignore
    public void textBoxAction(TextBoxWidget tbwe) {
    }


    /** Implements the PropertyChangeListener interface */
    public void propertyChange(PropertyChangeEvent evt) {
        if (_ignoreChanges) return;
        TextBoxWidget tbwe = getPosAngleTextBox();
        if (tbwe != null && getDataObject() != null) {
            final String newAngle = getDataObject().getPosAngleDegreesStr();
            if (!newAngle.equals(tbwe.getText())) {
                tbwe.setText(newAngle);
            }
        }
        tbwe = getExposureTimeTextBox();
        // Added this check because TReCS doesn't have an exposure time box!
        if (tbwe != null && getDataObject() != null) {
            final String newExpTime = getDataObject().getExposureTimeAsString();
            if (!newExpTime.equals(tbwe.getText())) {
                tbwe.setText(newExpTime);
            }
        }
    }

    /** Return the position angle text box */
    public abstract TextBoxWidget getPosAngleTextBox();

    /** Return the exposure time text box */
    public abstract TextBoxWidget getExposureTimeTextBox();

    /** Return the coadds text box, or null if not available. */
    public abstract TextBoxWidget getCoaddsTextBox();

    /** The default exposure time. Subclasses can override. **/
    protected double getDefaultExposureTime() {
        return 60.0;
    }

    /** If true, force an integer exposure time. Subclasses can override. **/
    protected boolean isForceIntegerExposureTime() {
        return false;
    }
}

