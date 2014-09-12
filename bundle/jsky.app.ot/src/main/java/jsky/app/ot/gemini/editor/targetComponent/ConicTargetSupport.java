package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.spModel.target.system.ConicTarget;
import edu.gemini.spModel.target.system.TypeBase;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.*;
import java.awt.*;

/**
 * Helper class for displaying values for a conic target
 * @deprecated replaced by {@link NonSiderealTargetSupport}
 */
@Deprecated
class ConicTargetSupport {

    // -- target parameters --

    // Groups together the widgets for one parameter for a conic target
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
    private ConicTargetParamWidgets[] _conicWidgets;


    // -- systems --

    // Maps the display name to the system type and the GUI labels to use.
    // Null labels are not used or displayed.
    private static class ConicSystem {
        ConicTarget.SystemType type;
        String[] labels; // parameter labels, indexed by param constants

        public ConicSystem(ConicTarget.SystemType type, String[] labels) {
            this.type = type;
            this.labels = labels;
        }
    }

    // number of different conic systems
    private static int _systemCount = 0;

    // System constants: Indexes into _conicSystems[] array.
    private static final int ASA_MAJOR_PLANET = _systemCount++;
    private static final int ASA_MINOR_PLANET = _systemCount++;
    private static final int ASA_COMET = _systemCount++;
    private static final int JPL_MAJOR_PLANET = _systemCount++;
    private static final int JPL_MINOR_BODY = _systemCount++;
    private static final int MPC_MINOR_PLANET = _systemCount++;
    private static final int MPC_COMET = _systemCount++;

    // array indexed by the constants above
    private ConicSystem[] _conicSystems;

    // array indexed by the constants above
    private String[] _conicSystemNames;


    // parameter labels for each system, indexed by [system][paramIndex]
    private String[][] _paramLabels;


    // The GUI layout panel
    private TelescopeForm _w;


    // ---

    // Initialize with the target list GUI class
    ConicTargetSupport(TelescopeForm w) {
        _w = w;

        _initParamLabels();
        _initConicWidgets();
        _initConicSystems();
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


    /** return an array of conic target system names */
    public String[] getConicSystemNames() {
        return _conicSystemNames;
    }

    // initialize the array of parameter labels for each system
    private void _initParamLabels() {
        _paramLabels = new String[_systemCount][_paramCount];

        _paramLabels[ASA_MAJOR_PLANET][EPOCHOFEL] = "Julian Date";
        _paramLabels[ASA_MAJOR_PLANET][ORBINC] = "Inclination";
        _paramLabels[ASA_MAJOR_PLANET][LONGASCNODE] = "Asc. Node";
        _paramLabels[ASA_MAJOR_PLANET][LONGOFPERI] = "Perihelion";
        _paramLabels[ASA_MAJOR_PLANET][ARGOFPERI] = null;
        _paramLabels[ASA_MAJOR_PLANET][MEANDIST] = "Mean Distance";
        _paramLabels[ASA_MAJOR_PLANET][PERIDIST] = null;
        _paramLabels[ASA_MAJOR_PLANET][ECCENTRICITY] = "Eccentricity";
        _paramLabels[ASA_MAJOR_PLANET][MEANLONG] = "Mean Longitude";
        _paramLabels[ASA_MAJOR_PLANET][MEANANOM] = null;
        _paramLabels[ASA_MAJOR_PLANET][DAILYMOT] = "Daily Motion";
        _paramLabels[ASA_MAJOR_PLANET][EPOCHOFPERI] = null;

        _paramLabels[ASA_MINOR_PLANET][EPOCHOFEL] = "EPOCH";
        _paramLabels[ASA_MINOR_PLANET][ORBINC] = "Inclination";
        _paramLabels[ASA_MINOR_PLANET][LONGASCNODE] = "Long. of Asc. Node";
        _paramLabels[ASA_MINOR_PLANET][LONGOFPERI] = null;
        _paramLabels[ASA_MINOR_PLANET][ARGOFPERI] = "Argument of Perihelion";
        _paramLabels[ASA_MINOR_PLANET][MEANDIST] = "Mean Distance";
        _paramLabels[ASA_MINOR_PLANET][PERIDIST] = null;
        _paramLabels[ASA_MINOR_PLANET][ECCENTRICITY] = "Eccentricity";
        _paramLabels[ASA_MINOR_PLANET][MEANLONG] = null;
        _paramLabels[ASA_MINOR_PLANET][MEANANOM] = "Mean Anomaly";
        _paramLabels[ASA_MINOR_PLANET][DAILYMOT] = null;
        _paramLabels[ASA_MINOR_PLANET][EPOCHOFPERI] = null;

        _paramLabels[ASA_COMET][EPOCHOFEL] = "Osc. Epoch";
        _paramLabels[ASA_COMET][ORBINC] = "Inclination";
        _paramLabels[ASA_COMET][LONGASCNODE] = "Long. of Asc.Node";
        _paramLabels[ASA_COMET][LONGOFPERI] = null;
        _paramLabels[ASA_COMET][ARGOFPERI] = "Arg. of Perihelion";
        _paramLabels[ASA_COMET][MEANDIST] = null;
        _paramLabels[ASA_COMET][PERIDIST] = "Perihelion Distance";
        _paramLabels[ASA_COMET][ECCENTRICITY] = "Eccentricity";
        _paramLabels[ASA_COMET][MEANLONG] = null;
        _paramLabels[ASA_COMET][MEANANOM] = null;
        _paramLabels[ASA_COMET][DAILYMOT] = null;
        _paramLabels[ASA_COMET][EPOCHOFPERI] = "Perihelion Time";

        _paramLabels[JPL_MAJOR_PLANET][EPOCHOFEL] = "EPOCH";
        _paramLabels[JPL_MAJOR_PLANET][ORBINC] = "IN";
        _paramLabels[JPL_MAJOR_PLANET][LONGASCNODE] = "OM";
        _paramLabels[JPL_MAJOR_PLANET][LONGOFPERI] = null;
        _paramLabels[JPL_MAJOR_PLANET][ARGOFPERI] = "W";
        _paramLabels[JPL_MAJOR_PLANET][MEANDIST] = "A";
        _paramLabels[JPL_MAJOR_PLANET][PERIDIST] = null;
        _paramLabels[JPL_MAJOR_PLANET][ECCENTRICITY] = "EC";
        _paramLabels[JPL_MAJOR_PLANET][MEANLONG] = null;
        _paramLabels[JPL_MAJOR_PLANET][MEANANOM] = "MA";
        _paramLabels[JPL_MAJOR_PLANET][DAILYMOT] = "N";
        _paramLabels[JPL_MAJOR_PLANET][EPOCHOFPERI] = null;

        _paramLabels[JPL_MINOR_BODY][EPOCHOFEL] = "EPOCH";
        _paramLabels[JPL_MINOR_BODY][ORBINC] = "IN";
        _paramLabels[JPL_MINOR_BODY][LONGASCNODE] = "OM";
        _paramLabels[JPL_MINOR_BODY][LONGOFPERI] = null;
        _paramLabels[JPL_MINOR_BODY][ARGOFPERI] = "W";
        _paramLabels[JPL_MINOR_BODY][MEANDIST] = null;
        _paramLabels[JPL_MINOR_BODY][PERIDIST] = "QR";
        _paramLabels[JPL_MINOR_BODY][ECCENTRICITY] = "EC";
        _paramLabels[JPL_MINOR_BODY][MEANLONG] = null;
        _paramLabels[JPL_MINOR_BODY][MEANANOM] = null;
        _paramLabels[JPL_MINOR_BODY][DAILYMOT] = null;
        _paramLabels[JPL_MINOR_BODY][EPOCHOFPERI] = "TP";

        _paramLabels[MPC_MINOR_PLANET][EPOCHOFEL] = "Epoch.";
        _paramLabels[MPC_MINOR_PLANET][ORBINC] = "Incl.";
        _paramLabels[MPC_MINOR_PLANET][LONGASCNODE] = "Node";
        _paramLabels[MPC_MINOR_PLANET][LONGOFPERI] = null;
        _paramLabels[MPC_MINOR_PLANET][ARGOFPERI] = "Peri.";
        _paramLabels[MPC_MINOR_PLANET][MEANDIST] = "a";
        _paramLabels[MPC_MINOR_PLANET][PERIDIST] = null;
        _paramLabels[MPC_MINOR_PLANET][ECCENTRICITY] = "e";
        _paramLabels[MPC_MINOR_PLANET][MEANLONG] = null;
        _paramLabels[MPC_MINOR_PLANET][MEANANOM] = "M";
        _paramLabels[MPC_MINOR_PLANET][DAILYMOT] = null;
        _paramLabels[MPC_MINOR_PLANET][EPOCHOFPERI] = null;

        _paramLabels[MPC_COMET][EPOCHOFEL] = "Epoch";
        _paramLabels[MPC_COMET][ORBINC] = "Incl.";
        _paramLabels[MPC_COMET][LONGASCNODE] = "Node";
        _paramLabels[MPC_COMET][LONGOFPERI] = null;
        _paramLabels[MPC_COMET][ARGOFPERI] = "Peri.";
        _paramLabels[MPC_COMET][MEANDIST] = null;
        _paramLabels[MPC_COMET][PERIDIST] = "q";
        _paramLabels[MPC_COMET][ECCENTRICITY] = "e";
        _paramLabels[MPC_COMET][MEANLONG] = null;
        _paramLabels[MPC_COMET][MEANANOM] = null;
        _paramLabels[MPC_COMET][DAILYMOT] = null;
        _paramLabels[MPC_COMET][EPOCHOFPERI] = "T";
    }


    /**
     * Returns the String value for the given JPL Horizons keyword.
     * see <a href="http://ssd.jpl.nasa.gov/horizons_doc.html#searching">JPL Horizons</a>
     *
     * @param key the JPL Horizons keyword (type SB at the telnet promp for a list)
     * @param target the ConicTarget holding the values
     */
    public String getJplParamValue(String key, ConicTarget target) {
        // From the JPL Horizons doc:
        //        ... acceptable label strings are defined as follows:
        //               EPOCH ....  Julian ephemeris date (CT) of osculating elements
        //               EC .......  Eccentricity
        //               QR .......  Perihelion distance in (AU)
        //               TP .......  Perihelion Julian date
        //               OM .......  Longitude of ascending node (DEGREES) wrt ecliptic
        //               W ........  Argument of perihelion (DEGREES) wrt ecliptic
        //               IN .......  Inclination (DEGREES) wrt ecliptic
        //
        //        Instead of {TP, QR}, {MA, A} or {MA,N} may be specified (not both):
        //               MA .......  Mean anomaly (DEGREES)
        //               A ........  Semi-major axis (AU)
        //               N ........  Mean motion (DEG/DAY)
        //
        //        Note that if you specify elements with MA, {TP, QR} will be computed from
        //        them. The program always uses TP and QR.

        TypeBase type = target.getSystemOption();
        if (type == ConicTarget.SystemType.ASA_MAJOR_PLANET) {
            if (key.equals("EPOCH")) { // Julian Date      <---- OT Labels
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // Inclination
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // Asc. Node
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // Perihelion
                return target.getPerihelion().getStringValue();
            } else if (key.equals("QR")) { // Mean Distance
                return target.getAQ().getStringValue();  // XXX an't use QR and MA
            } else if (key.equals("EC")) { // Eccentricity
                return String.valueOf(target.getE());
            } else if (key.equals("MA")) { // Mean Longitude
                return target.getLM().getStringValue();
            } else if (key.equals("N")) { // Daily Motion
                return target.getN().getStringValue();
            }
        } else if (type == ConicTarget.SystemType.ASA_MINOR_PLANET) {
            if (key.equals("EPOCH")) { // "Julian Date"
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // Inclination
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // Long. of Asc. Node
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // Argument of Perihelion
                return target.getPerihelion().getStringValue();
            } else if (key.equals("N")) { // Mean Distance
                return target.getAQ().getStringValue(); // XXX?
            } else if (key.equals("EC")) { // Eccentricity
                return String.valueOf(target.getE());
            } else if (key.equals("MA")) { // Mean Anomaly
                return target.getLM().getStringValue();
            }
        } else if (type == ConicTarget.SystemType.ASA_COMET) {
            if (key.equals("EPOCH")) { // Osc. Epoch
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // Inclination
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // Long. of Asc. Node
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // Arg. of Perihelion
                return target.getPerihelion().getStringValue();
            } else if (key.equals("QR")) { // Perihelion Distance
                return target.getAQ().getStringValue();
            } else if (key.equals("EC")) { // Eccentricity
                return String.valueOf(target.getE());
            } else if (key.equals("TP")) { // Perihelion Time
                return target.getEpochOfPeri().getStringValue();
            }
        } else if (type == ConicTarget.SystemType.JPL_MAJOR_PLANET) {
            if (key.equals("EPOCH")) { // EPOCH
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // IN
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // OM
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // W
                return target.getPerihelion().getStringValue();
            } else if (key.equals("QR")) { // Q
                return target.getAQ().getStringValue(); // XXX Can't use QR with MA
            } else if (key.equals("EC")) { // EC
                return String.valueOf(target.getE());
            } else if (key.equals("MA")) { // MA
                return target.getLM().getStringValue();
            } else if (key.equals("N")) { // N
                return target.getN().getStringValue();
            }
        } else if (type == ConicTarget.SystemType.JPL_MINOR_BODY) {
            if (key.equals("EPOCH")) { // EPOCH
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // IN
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // OM
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // W
                return target.getPerihelion().getStringValue();
            } else if (key.equals("QR")) { // Q
                return target.getAQ().getStringValue();
            } else if (key.equals("EC")) { // EC
                return String.valueOf(target.getE());
            } else if (key.equals("TP")) { // TP
                return target.getEpochOfPeri().getStringValue();
            }
        } else if (type == ConicTarget.SystemType.MPC_MINOR_PLANET) {
            if (key.equals("EPOCH")) { // Epoch
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // Incl.
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // Node
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // Peri.
                return target.getPerihelion().getStringValue();
            } else if (key.equals("A")) { // a
                return target.getAQ().getStringValue();
            } else if (key.equals("EC")) { // e
                return String.valueOf(target.getE());
            } else if (key.equals("MA")) { // M
                return target.getEpochOfPeri().getStringValue();
            }
        } else if (type == ConicTarget.SystemType.MPC_COMET) {
            if (key.equals("EPOCH")) { // Epoch
                return target.getEpoch().getStringValue();
            } else if (key.equals("IN")) { // Incl
                return target.getInclination().getStringValue();
            } else if (key.equals("OM")) { // Node
                return target.getANode().getStringValue();
            } else if (key.equals("W")) { // Peri
                return target.getPerihelion().getStringValue();
            } else if (key.equals("A")) { // a
                return target.getAQ().getStringValue();
            } else if (key.equals("EC")) { // e
                return String.valueOf(target.getE());
            } else if (key.equals("MA")) { // M
                return target.getEpochOfPeri().getStringValue();
            }
        }
        return null;
    }

    // initialize the array of widgets for each parameter
    private void _initConicWidgets() {
        _conicWidgets = new ConicTargetParamWidgets[_paramCount];
        _conicWidgets[EPOCHOFEL] = new ConicTargetParamWidgets(_w.epochofelLabel, _w.epochofel, _w.epochofelUnits);
        _conicWidgets[ORBINC] = new ConicTargetParamWidgets(_w.orbincLabel, _w.orbinc, _w.orbincUnits);
        _conicWidgets[LONGASCNODE] = new ConicTargetParamWidgets(_w.longascnodeLabel, _w.longascnode, _w.longascnodeUnits);
        _conicWidgets[LONGOFPERI] = new ConicTargetParamWidgets(_w.longofperiLabel, _w.longofperi, _w.longofperiUnits);
        _conicWidgets[ARGOFPERI] = new ConicTargetParamWidgets(_w.argofperiLabel, _w.argofperi, _w.argofperiUnits);
        _conicWidgets[MEANDIST] = new ConicTargetParamWidgets(_w.meandistLabel, _w.meandist, _w.meandistUnits);
        _conicWidgets[PERIDIST] = new ConicTargetParamWidgets(_w.peridistLabel, _w.peridist, _w.peridistUnits);
        _conicWidgets[ECCENTRICITY] = new ConicTargetParamWidgets(_w.eccentricityLabel, _w.eccentricity, _w.eccentricityUnits);
        _conicWidgets[MEANLONG] = new ConicTargetParamWidgets(_w.meanlongLabel, _w.meanlong, _w.meanlongUnits);
        _conicWidgets[MEANANOM] = new ConicTargetParamWidgets(_w.meananomLabel, _w.meananom, _w.meananomUnits);
        _conicWidgets[DAILYMOT] = new ConicTargetParamWidgets(_w.dailymotLabel, _w.dailymot, _w.dailymotUnits);
        _conicWidgets[EPOCHOFPERI] = new ConicTargetParamWidgets(_w.epochofperiLabel, _w.epochofperi, _w.epochofperiUnits);
    }


    // initialize the array of conic system information
    private void _initConicSystems() {
        _conicSystems = new ConicSystem[_paramCount];
        _conicSystems[ASA_MAJOR_PLANET] = new ConicSystem(
                ConicTarget.SystemType.ASA_MAJOR_PLANET,
                _paramLabels[ASA_MAJOR_PLANET]);
        _conicSystems[ASA_MINOR_PLANET] = new ConicSystem(
                ConicTarget.SystemType.ASA_MINOR_PLANET,
                _paramLabels[ASA_MINOR_PLANET]);
        _conicSystems[ASA_COMET] = new ConicSystem(
                ConicTarget.SystemType.ASA_COMET,
                _paramLabels[ASA_COMET]);
        _conicSystems[JPL_MAJOR_PLANET] = new ConicSystem(
                ConicTarget.SystemType.JPL_MAJOR_PLANET,
                _paramLabels[JPL_MAJOR_PLANET]);
        _conicSystems[JPL_MINOR_BODY] = new ConicSystem(
                ConicTarget.SystemType.JPL_MINOR_BODY,
                _paramLabels[JPL_MINOR_BODY]);
        _conicSystems[MPC_MINOR_PLANET] = new ConicSystem(
                ConicTarget.SystemType.MPC_MINOR_PLANET,
                _paramLabels[MPC_MINOR_PLANET]);
        _conicSystems[MPC_COMET] = new ConicSystem(
                ConicTarget.SystemType.MPC_COMET,
                _paramLabels[MPC_COMET]);

        // init array of display names
        _conicSystemNames = new String[_systemCount];
        for(int i = 0; i < _systemCount; i++) {
            _conicSystemNames[i] = _conicSystems[i].type.getName();
        }
    }


    /** Return the name of the {@link ConicTarget} system to use for the given display name */
    public String getSystem(String displayName) {
        for(int i = 0; i < _systemCount; i++) {
            if (_conicSystems[i].type.getName().equals(displayName)) {
                return _conicSystems[i].type.getName();
            }
        }
        return null;
    }

    /**
     * Display the given target in the GUI
     *
     * @param target the target position to display
     * @param system the target coordinate system {@link ConicTarget.SystemType#TYPES}
     */
    public void showConicTarget(ConicTarget target, String system) {
        for(int i = 0; i < _systemCount; i++) {
            if (_conicSystems[i].type.getName().equals(system)) {
                showConicTarget(target, _conicSystems[i]);
            }
        }
    }

    // Display the given target in the GUI
    private void showConicTarget(ConicTarget target, ConicSystem system) {
        int row = 0;
        int col = 0;
        for(int i = 0; i < _paramCount; i++) {
            String label = system.labels[i];
            if (label != null) {
                _conicWidgets[i].setVisible(true);
                _conicWidgets[i].setText(label);
                String s = _getTargetParamValueAsString(target, i);
                _conicWidgets[i].setValue(s);
                _conicWidgets[i].setPos(row, col++);
                if (col > 1) {
                    row++;
                    col = 0;
                }
            } else {
                _conicWidgets[i].setVisible(false);

            }
        }
        _w.nonsiderealPW.revalidate();
        _w.nonsiderealPW.repaint();
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
}
