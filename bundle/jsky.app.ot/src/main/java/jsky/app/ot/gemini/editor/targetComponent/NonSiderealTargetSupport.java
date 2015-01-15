package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.*;
import jsky.app.ot.gemini.editor.horizons.HorizonsService;
import jsky.app.ot.ui.util.TimeDocument;
import jsky.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

//$Id: NonSiderealTargetSupport.java 7515 2006-12-28 18:04:41Z anunez $
/**
 * Helper class for displaying and operating with non sidereal targets.
 * <p/>
 * Non sidereal targets can be {@link edu.gemini.spModel.target.system.ConicTarget} and
 * {@link edu.gemini.spModel.target.system.NamedTarget}
 *
 */
class NonSiderealTargetSupport {
    private static final DateFormat dateFormater = new SimpleDateFormat("dd/MMM/yyyy");
    private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss z");
    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        timeFormatter.setTimeZone(tz);
        dateFormater.setTimeZone(tz);
    }

    // -- target parameters --

    // Groups together the widgets for one parameter for conic targets
    private class ConicTargetParamWidgets {
        private JLabel _label;
        private NumberBoxWidget _entry;
        private JLabel _units;

        public ConicTargetParamWidgets(JLabel label, NumberBoxWidget entry, JLabel units) {
            _label = label;
            _entry = entry;
            _units = units;
        }

        public JLabel getLabel() {
            return _label;
        }

        public NumberBoxWidget getEntry() {
            return _entry;
        }

        public JLabel getUnits() {
            return _units;
        }

        public void setVisible(boolean b) {
            _label.setVisible(b);
            _entry.setVisible(b);
            _units.setVisible(b);
        }

        public void setText(String s) {
            _label.setText(s);
        }

        public void setValue(String s) {
            _entry.setText(s);
        }

        public void setToolTip(String s) {
            _entry.setToolTipText(s);
            _label.setToolTipText(s);
        }

        // Set the position in the grid: note the col arg is either
        // 0 or 1 and is adjusted to make room for the _label, _entry, and units.
        // The row arg strarts at 0 and is adjusted according to the first row used
        // by the param widgets in the GUI (2).
        public void setPos(int row, int col) {
            col *= 3;
            row += 2;
            _updateConstraints(_label, row, col++);
            _updateConstraints(_entry, row, col++);
            _updateConstraints(_units, row, col);
        }

        private void _updateConstraints(JComponent c, int row, int col) {
            GridBagLayout layout = (GridBagLayout)_w.nonsiderealPW.getLayout();
            GridBagConstraints contraints = layout.getConstraints(c);
            contraints.gridx = col;
            contraints.gridy = row;
            layout.setConstraints(c, contraints);
        }
    }

    // number of possible parameters
    private static int _paramCount = 0;

    // Parameter constants: Indexes into _conicWidgets[] array.
    private static final int EPOCHOFEL = _paramCount++;
    private static final int ORBINC = _paramCount++;
    private static final int LONGASCNODE = _paramCount++;
    private static final int LONGOFPERI = _paramCount++;
    private static final int ARGOFPERI = _paramCount++;
    private static final int MEANDIST = _paramCount++;
    private static final int PERIDIST = _paramCount++;
    private static final int ECCENTRICITY = _paramCount++;
    private static final int MEANLONG = _paramCount++;
    private static final int MEANANOM = _paramCount++;
    private static final int DAILYMOT = _paramCount++;
    private static final int EPOCHOFPERI = _paramCount++;

    // array indexed by the constants above
    private NonSiderealTargetSupport.ConicTargetParamWidgets[] _conicWidgets;

    // parameter labels and ToolTips for each system, indexed by [system][paramIndex][0] and
    // [system][paramIndex][1]
    private String[][][] _paramLabels;


    // The GUI layout panel
    private TelescopeForm _w;


    // If true, ignore events that trigger a reset of the cache
    private boolean _ignoreResetCacheEvents = false;


    // Current position being edited
    private SPTarget _curPos;


    // ---

    // Initialize with the target list GUI class
    NonSiderealTargetSupport(TelescopeForm w, SPTarget curPos) {
        _w = w;
        _curPos = curPos;
        _initParamLabels();
        _initConicWidgets();
    }

    /** Add the given listener to all the entry widgets */
    public void initListeners(TextBoxWidgetWatcher watcher) {
        for(int i = 0; i < _paramCount; i++) {
            _conicWidgets[i].getEntry().addWatcher(watcher);
        }
    }

    /**
     * Set the value of the given target field to whatever is contained in the given
     * widget. The widget is used to determine which field to set. This method is called
     * whenever the user types a value into one of the conic target entry widgets.
     */
    public void setConicPos(ConicTarget target, NumberBoxWidget nbw) {
        double value;
        try {
            value = Double.valueOf(nbw.getValue());
        } catch (Exception ex) {
            return;
        }
        for(int i = 0; i < _paramCount; i++) {
            if (_conicWidgets[i].getEntry() == nbw) {
                _setConicPos(target, i, value);
                return;
            }
        }
    }

    // Set the value of the given param to the given value
    private void _setConicPos(ConicTarget target, int paramIndex, double value) {
        if (paramIndex == EPOCHOFEL) {
            target.getEpoch().setValue(value);
        }
        if (paramIndex == ORBINC) {
            target.getInclination().setValue(value);
        }
        if (paramIndex == LONGASCNODE) {
            target.getANode().setValue(value);
        }
        if (paramIndex == LONGOFPERI) {
            target.getPerihelion().setValue(value);
        }
        if (paramIndex == ARGOFPERI) {
            target.getPerihelion().setValue(value);
        }
        if (paramIndex == MEANDIST) {
            target.getAQ().setValue(value);
        }
        if (paramIndex == PERIDIST) {
            target.getAQ().setValue(value);
        }
        if (paramIndex == ECCENTRICITY) {
            target.setE(value);
        }
        if (paramIndex == MEANLONG) {
            target.getLM().setValue(value);
        }
        if (paramIndex == MEANANOM) {
            target.getLM().setValue(value);
        }
        if (paramIndex == DAILYMOT) {
            target.getN().setValue(value);
        }
        if (paramIndex == EPOCHOFPERI) {
            target.getEpochOfPeri().setValue(value);
        }
    }


    // initialize the array of parameter labels for each system
    private void _initParamLabels() {
        _paramLabels = new String[ITarget.Tag.values().length][_paramCount][2];

        _paramLabels[ITarget.Tag.NAMED.ordinal()][EPOCHOFEL] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][ORBINC] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][LONGASCNODE] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][LONGOFPERI] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][ARGOFPERI] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][MEANDIST] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][PERIDIST] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][ECCENTRICITY] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][MEANLONG] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][MEANANOM] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][DAILYMOT] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][EPOCHOFPERI] = new String[]{null, null};

        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][EPOCHOFEL] = new String[]{"EPOCH", "Orbital Element Epoch"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][ORBINC] = new String[]{"IN", "Inclination"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][LONGASCNODE] = new String[]{"OM", "Longitude of Ascending Node"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][LONGOFPERI] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][ARGOFPERI] = new String[]{"W", "Argument of Perihelion"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][MEANDIST] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][PERIDIST] = new String[]{"QR", "Perihelion Distance"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][ECCENTRICITY] = new String[]{"EC", "Eccentricity"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][MEANLONG] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][MEANANOM] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][DAILYMOT] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][EPOCHOFPERI] = new String[]{"TP", "Time of Perihelion Passage"};

        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][EPOCHOFEL] = new String[]{"EPOCH", "Orbital Element Epoch"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][ORBINC] = new String[]{"IN", "Inclination"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][LONGASCNODE] = new String[]{"OM", "Longitude of Ascending Node"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][LONGOFPERI] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][ARGOFPERI] = new String[]{"W", "Argument of Perihelion"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][MEANDIST] = new String[]{"A", "Semi-major Axis"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][PERIDIST] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][ECCENTRICITY] = new String[]{"EC", "Eccentricity"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][MEANLONG] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][MEANANOM] = new String[]{"MA", "Mean Anomaly"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][DAILYMOT] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][EPOCHOFPERI] = new String[]{null, null};

    }

    // initialize the array of widgets for each parameter
    private void _initConicWidgets() {
        _conicWidgets = new NonSiderealTargetSupport.ConicTargetParamWidgets[_paramCount];
        _conicWidgets[EPOCHOFEL] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.epochofelLabel, _w.epochofel, _w.epochofelUnits);
        _conicWidgets[ORBINC] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.orbincLabel, _w.orbinc, _w.orbincUnits);
        _conicWidgets[LONGASCNODE] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.longascnodeLabel, _w.longascnode, _w.longascnodeUnits);
        _conicWidgets[LONGOFPERI] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.longofperiLabel, _w.longofperi, _w.longofperiUnits);
        _conicWidgets[ARGOFPERI] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.argofperiLabel, _w.argofperi, _w.argofperiUnits);
        _conicWidgets[MEANDIST] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.meandistLabel, _w.meandist, _w.meandistUnits);
        _conicWidgets[PERIDIST] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.peridistLabel, _w.peridist, _w.peridistUnits);
        _conicWidgets[ECCENTRICITY] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.eccentricityLabel, _w.eccentricity, _w.eccentricityUnits);
        _conicWidgets[MEANLONG] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.meanlongLabel, _w.meanlong, _w.meanlongUnits);
        _conicWidgets[MEANANOM] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.meananomLabel, _w.meananom, _w.meananomUnits);
        _conicWidgets[DAILYMOT] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.dailymotLabel, _w.dailymot, _w.dailymotUnits);
        _conicWidgets[EPOCHOFPERI] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.epochofperiLabel, _w.epochofperi, _w.epochofperiUnits);
    }


    /**
     * Display the given target in the GUI
     * @param target the target position to display
     **/
    public void showNonSiderealTarget(NonSiderealTarget target) {
        for (ITarget.Tag tag: ITarget.Tag.values()) {
            if (tag == target.getTag()) {

                // Update all the conic parameter widgets
                int row = 0;
                int col = 0;
                for (int i1 = 0; i1 < _paramCount; i1++) {
                    final ConicTargetParamWidgets cw = _conicWidgets[i1];
                    final String[][] labels = _paramLabels[tag.ordinal()];
                    final String label = labels[i1][0];
                    final String toolTip = labels[i1][1];
                    if (label != null) {
                        cw.setVisible(true);
                        cw.setText(label);
                        cw.setToolTip(toolTip);
                        if (target instanceof ConicTarget) {
                            String s = _getTargetParamValueAsString((ConicTarget) target, i1);
                            cw.setValue(s);
                        }
                        cw.setPos(row, col++);
                        if (col > 1) {
                            row++;
                            col = 0;
                        }
                    } else {
                        cw.setVisible(false);
                    }
                }

                // Update the RA and Dec
                _w.xaxis.setText(target.c1ToString());
                _w.yaxis.setText(target.c2ToString());

                // Update the valid-at date
                Date date = target.getDateForPosition();
                JTextField tf = (JTextField)_w.calendarTime.getEditor().getEditorComponent();
                TimeDocument td = (TimeDocument)tf.getDocument();
                //if the date is null  use the current time
                if (date == null) {
                    date = new Date();
                }
                _w.calendarDate.setDate(date);
                td.setTime(EdCompTargetList.timeFormatter.format(date));

                // Update the solar system stuff
                if (target instanceof NamedTarget) {
                    _w.planetsPanel.setVisible(true);
                    NamedTarget pt = (NamedTarget) target;
                    NamedTarget.SolarObject solarObject = pt.getSolarObject();
                    if (solarObject != null) {
                        int pos = solarObject.ordinal();
                        if (pos < _w.planetButtons.length) {
                            _w.planetButtons[pos].setSelected(true);
                        }
                    }
                } else {
                    _w.planetsPanel.setVisible(false);
                }

                // And revalidate everything
                _w.nonsiderealPW.revalidate();
                _w.nonsiderealPW.repaint();
                break;

            }
        }
    }


    // Return the current value of the given parameter as a string
    private String _getTargetParamValueAsString(ConicTarget target, int paramIndex) {
        double d = _getTargetParamvalue(target, paramIndex);
        return String.valueOf(d);
    }
    // Return the current value of the given parameter
    private double _getTargetParamvalue(ConicTarget target, int paramIndex) {
        if (paramIndex == EPOCHOFEL) {
            return target.getEpoch().getValue();
        }
        if (paramIndex == ORBINC) {
            return target.getInclination().getValue();
        }
        if (paramIndex == LONGASCNODE) {
            return target.getANode().getValue();
        }
        if (paramIndex == LONGOFPERI) {
            return target.getPerihelion().getValue();
        }
        if (paramIndex == ARGOFPERI) {
            return target.getPerihelion().getValue();
        }
        if (paramIndex == MEANDIST) {
            return target.getAQ().getValue();
        }
        if (paramIndex == PERIDIST) {
            return target.getAQ().getValue();
        }
        if (paramIndex == ECCENTRICITY) {
            return target.getE();
        }
        if (paramIndex == MEANLONG) {
            return target.getLM().getValue();
        }
        if (paramIndex == MEANANOM) {
            return target.getLM().getValue();
        }
        if (paramIndex == DAILYMOT) {
            return target.getN().getValue();
        }
        if (paramIndex == EPOCHOFPERI) {
            return target.getEpochOfPeri().getValue();
        }

        return 0.;
    }

    private DropDownListBoxWidgetWatcher orbitalElementFormatWatcher = new DropDownListBoxWidgetWatcher()  {
        public void dropDownListBoxAction(DropDownListBoxWidget dd, int i, String val) {
            _curPos.setCoordSys((ITarget.Tag) _w.orbitalElementFormat.getSelectedItem());
            if (!_ignoreResetCacheEvents) {
                HorizonsService.resetCache();
            }
        }
    };

    // Initialize the Orbital Element Format menu
    void initOrbitalElementFormatChoices() {
        _w.orbitalElementFormat.deleteWatcher(orbitalElementFormatWatcher);
        _w.orbitalElementFormat.clear();
        _w.orbitalElementFormat.addChoice(ITarget.Tag.JPL_MINOR_BODY);
        _w.orbitalElementFormat.addChoice(ITarget.Tag.MPC_MINOR_PLANET);
        _w.orbitalElementFormat.addChoice(ITarget.Tag.NAMED);
        _w.orbitalElementFormat.addWatcher(orbitalElementFormatWatcher);
    }

    void showOrbitalElementFormat() {
        _w.orbitalElementFormat.deleteWatcher(orbitalElementFormatWatcher);
        _w.orbitalElementFormat.setSelectedItem(_curPos.getTarget().getTag());
        _w.orbitalElementFormat.addWatcher(orbitalElementFormatWatcher);
    }

    public void updatePos(SPTarget target) {
        _curPos = target;
    }

    public void ignoreResetCacheEvents(boolean b) {
        _ignoreResetCacheEvents = b;
    }

}
