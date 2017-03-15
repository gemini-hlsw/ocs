package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.TextBoxWidget;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * The base class for all instrument components.  It includes support for
 * a rotation angle text box, something all instruments support.
 *
 * NOTE: We do not use the getPosAngleTextBox mechanism any more with instrument editors that support the
 * parallactic angle feature, because the logic for this is fully contained in PositionAnglePanel.
 * Thus, this method should return null if a PositionAnglePanel is used.
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

    @Override
    protected void init() {
        if (getDataObject() != null) {
            getDataObject().addPropertyChangeListener(this);

            if (getPosAngleTextBox() != null) {
                getPosAngleTextBox().setText(getDataObject().getPosAngleDegreesStr());
                getPosAngleTextBox().addWatcher(this);

                // We now ignore changes to the pos angle if the text field is being edited.
                // As a result, make sure everything is synched when editing starts / stops.
                getPosAngleTextBox().addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(final FocusEvent e) {
                        updatePosAngle(false);
                    }
                    @Override public void focusGained(final FocusEvent e) {
                        updatePosAngle(false);
                    }
                });
                getPosAngleTextBox().addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(final KeyEvent e) {
                        super.keyPressed(e);
                        if (e.getKeyCode() == KeyEvent.VK_ENTER)
                            updatePosAngle(true);
                    }
                });
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

    /**
     * A key was pressed in the given TextBoxWidget.
     */
    @Override
    public void textBoxKeyPress(final TextBoxWidget tbwe) {
        if (getDataObject() != null) {
            _ignoreChanges = true;
            try {
                if (tbwe == getPosAngleTextBox()) {
                    final double defaultPositionAngle = 0.0;
                    getDataObject().setPosAngleDegrees(tbwe.getDoubleValue(defaultPositionAngle));
                } else if (tbwe == getExposureTimeTextBox()) {
                    final double expTime = tbwe.getDoubleValue(getDefaultExposureTime());
                    getDataObject().setExposureTime(expTime);
                } else if (tbwe == getCoaddsTextBox()) {
                    final int defaultCoadds = 1;
                    getDataObject().setCoadds(tbwe.getIntegerValue(defaultCoadds));
                }
            } finally {
                _ignoreChanges = false;
            }
        }
    }

    // Generic method to copy a data object value to a text field, possibly contingent on some check.
    private void updateField(final TextBoxWidget tbwe,
                             final Supplier<Boolean> additionalCheck,
                             final Function<T,String> newValueExtractor) {
        if (!_ignoreChanges) {
            final T dObj = getDataObject();
            if (tbwe != null && dObj != null && additionalCheck.get()) {
                final String newValue = newValueExtractor.apply(dObj);
                if (!newValue.equals(tbwe.getText())) {
                    tbwe.setText(newValue);
                }
            }
        }
    }
    private void updateField(final TextBoxWidget tbwe,
                             final Function<T,String> newValueExtractor) {
        updateField(tbwe, () -> true, newValueExtractor);
    }

    // Copy the data model pos angle value to the pos angle text field.
    private void updatePosAngle(final boolean ignoreFocus) {
        final TextBoxWidget posAngleTextBox = getPosAngleTextBox();
        updateField(posAngleTextBox, () -> ignoreFocus || !posAngleTextBox.hasFocus(), T::getPosAngleDegreesStr);
    }

    // Copy the data model exposure time value to the exposure time text field.
    private void updateExpTime() {
        updateField(getExposureTimeTextBox(), T::getExposureTimeAsString);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        updatePosAngle(false);
        updateExpTime();
    }

    /**
     * Return the position angle text box
     * Providing null is a temporary measure that indicates that external code handles the position angle.
     * This will go away when all position angle handling for editors is done through the PositionAnglePanel.
     * TODO: Remove this and all references.
     */
    public TextBoxWidget getPosAngleTextBox() { return null; }

    /** Return the exposure time text box */
    public abstract TextBoxWidget getExposureTimeTextBox();

    /** Return the coadds text box, or null if not available. */
    public abstract TextBoxWidget getCoaddsTextBox();

    /** The default exposure time. Subclasses can override. **/
    protected double getDefaultExposureTime() {
        return 60.0;
    }
}

