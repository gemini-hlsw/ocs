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


    // -- systems --

    // Maps the display name to the system type and the GUI labels to use.
    // Null labels are not used or displayed.
    public static class NonSiderealSystem {
        TypeBase type;
        String displayName;
        String[][] labels; // parameter labels/tooltips, indexed by param constants

        public NonSiderealSystem(TypeBase type, String[][] labels) {
            this(type, labels, type.getName());
        }

        public NonSiderealSystem(TypeBase type, String[][] labels, String displayValue) {
            this.type = type;
            this.labels = labels;
            this.displayName = displayValue;
        }

        public String toString() {
            return displayName;
        }
    }

    // number of different non sidereal systems
    private static int _systemCount = 0;

    // System constants: Indexes into _nonSiderealSystems[] array.
    public static final int JPL_COMET = _systemCount++;
    public static final int JPL_MINOR_PLANET = _systemCount++;
    public static final int MAJOR_PLANET = _systemCount++;


    // array indexed by the constants above
    private NonSiderealTargetSupport.NonSiderealSystem[] _nonSiderealSystems;

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
        _initNonSiderealSystems();
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

    /**
     * Get the available Non Sidereal Systems
     */
    public NonSiderealTargetSupport.NonSiderealSystem[] getNonSiderealSystems() {
        return _nonSiderealSystems;
    }

    /**
     * Return the ConicSystem associated to the given base
     */

    public NonSiderealTargetSupport.NonSiderealSystem getNonSiderealSystem(TypeBase base) {
        for (NonSiderealTargetSupport.NonSiderealSystem s : _nonSiderealSystems) {
            if (s.type == base) {
                return s;
            }
        }
        return null;
    }

    // initialize the array of parameter labels for each system
    private void _initParamLabels() {
        _paramLabels = new String[_systemCount][_paramCount][2];

        _paramLabels[MAJOR_PLANET][EPOCHOFEL] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][ORBINC] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][LONGASCNODE] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][LONGOFPERI] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][ARGOFPERI] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][MEANDIST] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][PERIDIST] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][ECCENTRICITY] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][MEANLONG] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][MEANANOM] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][DAILYMOT] = new String[]{null, null};
        _paramLabels[MAJOR_PLANET][EPOCHOFPERI] = new String[]{null, null};

        _paramLabels[JPL_COMET][EPOCHOFEL] = new String[]{"EPOCH", "Orbital Element Epoch"};
        _paramLabels[JPL_COMET][ORBINC] = new String[]{"IN", "Inclination"};
        _paramLabels[JPL_COMET][LONGASCNODE] = new String[]{"OM", "Longitude of Ascending Node"};
        _paramLabels[JPL_COMET][LONGOFPERI] = new String[]{null, null};
        _paramLabels[JPL_COMET][ARGOFPERI] = new String[]{"W", "Argument of Perihelion"};
        _paramLabels[JPL_COMET][MEANDIST] = new String[]{null, null};
        _paramLabels[JPL_COMET][PERIDIST] = new String[]{"QR", "Perihelion Distance"};
        _paramLabels[JPL_COMET][ECCENTRICITY] = new String[]{"EC", "Eccentricity"};
        _paramLabels[JPL_COMET][MEANLONG] = new String[]{null, null};
        _paramLabels[JPL_COMET][MEANANOM] = new String[]{null, null};
        _paramLabels[JPL_COMET][DAILYMOT] = new String[]{null, null};
        _paramLabels[JPL_COMET][EPOCHOFPERI] = new String[]{"TP", "Time of Perihelion Passage"};

        _paramLabels[JPL_MINOR_PLANET][EPOCHOFEL] = new String[]{"EPOCH", "Orbital Element Epoch"};
        _paramLabels[JPL_MINOR_PLANET][ORBINC] = new String[]{"IN", "Inclination"};
        _paramLabels[JPL_MINOR_PLANET][LONGASCNODE] = new String[]{"OM", "Longitude of Ascending Node"};
        _paramLabels[JPL_MINOR_PLANET][LONGOFPERI] = new String[]{null, null};
        _paramLabels[JPL_MINOR_PLANET][ARGOFPERI] = new String[]{"W", "Argument of Perihelion"};
        _paramLabels[JPL_MINOR_PLANET][MEANDIST] = new String[]{"A", "Semi-major Axis"};
        _paramLabels[JPL_MINOR_PLANET][PERIDIST] = new String[]{null, null};
        _paramLabels[JPL_MINOR_PLANET][ECCENTRICITY] = new String[]{"EC", "Eccentricity"};
        _paramLabels[JPL_MINOR_PLANET][MEANLONG] = new String[]{null, null};
        _paramLabels[JPL_MINOR_PLANET][MEANANOM] = new String[]{"MA", "Mean Anomaly"};
        _paramLabels[JPL_MINOR_PLANET][DAILYMOT] = new String[]{null, null};
        _paramLabels[JPL_MINOR_PLANET][EPOCHOFPERI] = new String[]{null, null};

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


    // initialize the array of Non Sidereal system information
    private void _initNonSiderealSystems() {
        _nonSiderealSystems = new NonSiderealTargetSupport.NonSiderealSystem[_systemCount];
        _nonSiderealSystems[MAJOR_PLANET] = new NonSiderealTargetSupport.NonSiderealSystem(
                NamedTarget.SystemType.SOLAR_OBJECT,
                _paramLabels[MAJOR_PLANET], "Solar System Object");
        _nonSiderealSystems[JPL_COMET] = new NonSiderealTargetSupport.NonSiderealSystem(
                ConicTarget.SystemType.JPL_MINOR_BODY,
                _paramLabels[JPL_COMET], "JPL Comet");
        _nonSiderealSystems[JPL_MINOR_PLANET] = new NonSiderealTargetSupport.NonSiderealSystem(
                ConicTarget.SystemType.MPC_MINOR_PLANET,
                _paramLabels[JPL_MINOR_PLANET], "JPL Minor Planet");
    }


    /**
     * Display the given target in the GUI. Offers an option to not update the date where the coordinates
     * are valid, useful when getting positions for new times
     *
     * @param target the target position to display
     * @param system the target coordinate system {@link edu.gemini.spModel.target.system.ConicTarget.SystemType#TYPES}
     * @param updateDate if the date for the current position should be updated
     */
    public void showNonSiderealTarget(NonSiderealTarget target, String system, boolean updateDate) {
        for(int i = 0; i < _systemCount; i++) {
            if (_nonSiderealSystems[i].type.getName().equals(system)) {
                showNonSiderealTarget(target, _nonSiderealSystems[i], updateDate);
            }
        }
    }

    /**
     * Display the given target in the GUI
     * @param target the target position to display
     * @param system the target coordinate system {@link edu.gemini.spModel.target.system.ConicTarget.SystemType#TYPES}
     **/
    public void showNonSiderealTarget(NonSiderealTarget target, String system) {
        showNonSiderealTarget(target, system, true);
    }

    // Display the given target in the GUI
    private void showNonSiderealTarget(NonSiderealTarget target, NonSiderealTargetSupport.NonSiderealSystem system, boolean updateDate) {
        int row = 0;
        int col = 0;
        for (int i = 0; i < _paramCount; i++) {
            String label = system.labels[i][0];
            String toolTip = system.labels[i][1];
            if (label != null) {
                _conicWidgets[i].setVisible(true);
                _conicWidgets[i].setText(label);
                _conicWidgets[i].setToolTip(toolTip);
                if (target instanceof ConicTarget) {
                    String s = _getTargetParamValueAsString((ConicTarget) target, i);
                    _conicWidgets[i].setValue(s);
                }
                _conicWidgets[i].setPos(row, col++);
                if (col > 1) {
                    row++;
                    col = 0;
                }
            } else {
                _conicWidgets[i].setVisible(false);
            }
        }
        _updatePosition(target, updateDate);
        _refreshPlanetPanel(target);
        //Only show the Planet Panel when the System is Major Planet
        _w.nonsiderealPW.revalidate();
        _w.nonsiderealPW.repaint();
    }

    /**
     * Updates the Position at a given Time for a Non Sidereal Target in the UI
     * @param target The NonSideralTarget to show
     */
    private void _updatePosition(NonSiderealTarget target, boolean updateDate) {
        //First the position
        _w.xaxis.setText(target.c1ToString());
        _w.yaxis.setText(target.c2ToString());
        //if we need to update the date
        if (updateDate) {
            //now the time if available
            Date date = target.getDateForPosition();
            JTextField tf = (JTextField)_w.calendarTime.getEditor().getEditorComponent();
            TimeDocument td = (TimeDocument)tf.getDocument();
            //if the date is null  use the current time
            if (date == null) {
                date = new Date();
            }
            _w.calendarDate.setDate(date);
            td.setTime(EdCompTargetList.timeFormatter.format(date));
        }
    }


    //refresh the Solar System Panel depending on the system type of the Non Sidereal Target
    private void _refreshPlanetPanel(CoordinateSystem target) {
        boolean isSolarObject = target.getSystemOption() == NamedTarget.SystemType.SOLAR_OBJECT;
        _w.planetsPanel.setVisible(isSolarObject);
        if (isSolarObject) { //refresh the content of the panel
            if (target instanceof NamedTarget) {
                NamedTarget pt = (NamedTarget)target;
                NamedTarget.SolarObject solarObject = pt.getSolarObject();
                if (solarObject != null) {
                    int pos = solarObject.ordinal();
                    if (pos < _w.planetButtons.length) {
                        _w.planetButtons[pos].setSelected(true);
                    }
                }
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
            if (_curPos.getTarget() instanceof ConicTarget) {
                if(((ConicTarget) _curPos.getTarget()).getSystemOption().getTypeCode() !=
                        ((NonSiderealTargetSupport.NonSiderealSystem)_w.orbitalElementFormat.getSelectedItem()).type.getTypeCode()){
                    //if we change the element type in the dropdown menu, we clean all the data, by creating a new ConicTarget
                    //and copying the name and the selected type(if we don't copy the selected type, then we can't modify by hand the dropdown menu)
                    int typecode = ((NonSiderealTargetSupport.NonSiderealSystem)_w.orbitalElementFormat.getSelectedItem()).type.getTypeCode();
                    String name =  ((ConicTarget) _curPos.getTarget()).getName();
                    _curPos.setTarget(new ConicTarget(ConicTarget.SystemType.TYPES[typecode]));
                    ((ConicTarget) _curPos.getTarget()).setName(name);
                }
            }
            _curPos.setCoordSys(((NonSiderealTargetSupport.NonSiderealSystem)_w.orbitalElementFormat.getSelectedItem()).type.getName());
            if (!_ignoreResetCacheEvents) {
                HorizonsService.resetCache();
            }
        }
    };

    // Initialize the Orbital Element Format menu
    private void _initOrbitalElementFormatChoices() {
        _w.orbitalElementFormat.deleteWatcher(orbitalElementFormatWatcher);
        _w.orbitalElementFormat.clear();
        NonSiderealTargetSupport.NonSiderealSystem[] systems = getNonSiderealSystems();

        for (NonSiderealTargetSupport.NonSiderealSystem system : systems) {
            _w.orbitalElementFormat.addChoice(system);
        }
        _w.orbitalElementFormat.addWatcher(orbitalElementFormatWatcher);
    }

    void showOrbitalElementFormat() {
        NonSiderealTargetSupport.NonSiderealSystem sytem = getNonSiderealSystem(_curPos.getCoordSys());
        if (sytem != null) {
            _w.orbitalElementFormat.deleteWatcher(orbitalElementFormatWatcher);
            _w.orbitalElementFormat.setSelectedItem(sytem);
            _w.orbitalElementFormat.addWatcher(orbitalElementFormatWatcher);
        } else {
            DialogUtil.error("Target with obsolete or unknown orbital format: " + _curPos.getCoordSys().getName());
        }
    }

    /**
     * Populates the system choices with the non sidereal stuff
     */
    public void initSystemChoices(String choice) {
         _w.system.addChoice(choice); // special case for nonsidereal targets
        _initOrbitalElementFormatChoices();
    }

    public void updatePos(SPTarget target) {
        _curPos = target;
    }

    public void ignoreResetCacheEvents(boolean b) {
        _ignoreResetCacheEvents = b;
    }

}
