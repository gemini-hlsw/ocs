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

    enum Param {
        EPOCHOFEL,
        ORBINC,
        LONGASCNODE,
        LONGOFPERI,
        ARGOFPERI,
        MEANDIST,
        PERIDIST,
        ECCENTRICITY,
        MEANLONG,
        MEANANOM,
        DAILYMOT,
        EPOCHOFPERI
    }

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
        for (Param p: Param.values()) {
            _conicWidgets[p.ordinal()].getEntry().addWatcher(watcher);
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
        for (Param p: Param.values()) {
            if (_conicWidgets[p.ordinal()].getEntry() == nbw) {
                _setConicPos(target, p, value);
                return;
            }
        }
    }

    // Set the value of the given param to the given value
    private void _setConicPos(ConicTarget target, Param param, double value) {
        switch (param) {
            case EPOCHOFEL:    target.getEpoch().setValue(value); break;
            case ORBINC:       target.getInclination().setValue(value); break;
            case LONGASCNODE:  target.getANode().setValue(value); break;
            case LONGOFPERI:   target.getPerihelion().setValue(value); break;
            case ARGOFPERI:    target.getPerihelion().setValue(value); break;
            case MEANDIST:     target.getAQ().setValue(value); break;
            case PERIDIST:     target.getAQ().setValue(value); break;
            case ECCENTRICITY: target.setE(value); break;
            case MEANLONG:     target.getLM().setValue(value); break;
            case MEANANOM:     target.getLM().setValue(value); break;
            case DAILYMOT:     target.getN().setValue(value); break;
            case EPOCHOFPERI:  target.getEpochOfPeri().setValue(value); break;
        }
    }


    // initialize the array of parameter labels for each system
    private void _initParamLabels() {
        _paramLabels = new String[ITarget.Tag.values().length][Param.values().length][2];

        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.EPOCHOFEL.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.ORBINC.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.LONGASCNODE.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.LONGOFPERI.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.ARGOFPERI.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.MEANDIST.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.PERIDIST.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.ECCENTRICITY.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.MEANLONG.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.MEANANOM.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.DAILYMOT.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.NAMED.ordinal()][Param.EPOCHOFPERI.ordinal()] = new String[]{null, null};

        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.EPOCHOFEL.ordinal()] = new String[]{"EPOCH", "Orbital Element Epoch"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.ORBINC.ordinal()] = new String[]{"IN", "Inclination"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.LONGASCNODE.ordinal()] = new String[]{"OM", "Longitude of Ascending Node"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.LONGOFPERI.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.ARGOFPERI.ordinal()] = new String[]{"W", "Argument of Perihelion"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.MEANDIST.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.PERIDIST.ordinal()] = new String[]{"QR", "Perihelion Distance"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.ECCENTRICITY.ordinal()] = new String[]{"EC", "Eccentricity"};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.MEANLONG.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.MEANANOM.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.DAILYMOT.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.JPL_MINOR_BODY.ordinal()][Param.EPOCHOFPERI.ordinal()] = new String[]{"TP", "Time of Perihelion Passage"};

        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.EPOCHOFEL.ordinal()] = new String[]{"EPOCH", "Orbital Element Epoch"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.ORBINC.ordinal()] = new String[]{"IN", "Inclination"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.LONGASCNODE.ordinal()] = new String[]{"OM", "Longitude of Ascending Node"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.LONGOFPERI.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.ARGOFPERI.ordinal()] = new String[]{"W", "Argument of Perihelion"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.MEANDIST.ordinal()] = new String[]{"A", "Semi-major Axis"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.PERIDIST.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.ECCENTRICITY.ordinal()] = new String[]{"EC", "Eccentricity"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.MEANLONG.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.MEANANOM.ordinal()] = new String[]{"MA", "Mean Anomaly"};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.DAILYMOT.ordinal()] = new String[]{null, null};
        _paramLabels[ITarget.Tag.MPC_MINOR_PLANET.ordinal()][Param.EPOCHOFPERI.ordinal()] = new String[]{null, null};


    }

    // initialize the array of widgets for each parameter
    private void _initConicWidgets() {
        _conicWidgets = new NonSiderealTargetSupport.ConicTargetParamWidgets[Param.values().length];
        _conicWidgets[Param.EPOCHOFEL.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.epochofelLabel, _w.epochofel, _w.epochofelUnits);
        _conicWidgets[Param.ORBINC.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.orbincLabel, _w.orbinc, _w.orbincUnits);
        _conicWidgets[Param.LONGASCNODE.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.longascnodeLabel, _w.longascnode, _w.longascnodeUnits);
        _conicWidgets[Param.LONGOFPERI.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.longofperiLabel, _w.longofperi, _w.longofperiUnits);
        _conicWidgets[Param.ARGOFPERI.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.argofperiLabel, _w.argofperi, _w.argofperiUnits);
        _conicWidgets[Param.MEANDIST.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.meandistLabel, _w.meandist, _w.meandistUnits);
        _conicWidgets[Param.PERIDIST.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.peridistLabel, _w.peridist, _w.peridistUnits);
        _conicWidgets[Param.ECCENTRICITY.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.eccentricityLabel, _w.eccentricity, _w.eccentricityUnits);
        _conicWidgets[Param.MEANLONG.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.meanlongLabel, _w.meanlong, _w.meanlongUnits);
        _conicWidgets[Param.MEANANOM.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.meananomLabel, _w.meananom, _w.meananomUnits);
        _conicWidgets[Param.DAILYMOT.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.dailymotLabel, _w.dailymot, _w.dailymotUnits);
        _conicWidgets[Param.EPOCHOFPERI.ordinal()] = new NonSiderealTargetSupport.ConicTargetParamWidgets(_w.epochofperiLabel, _w.epochofperi, _w.epochofperiUnits);
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
                for (Param p: Param.values()) {
                    final int i1 = p.ordinal();
                    final ConicTargetParamWidgets cw = _conicWidgets[i1];
                    final String[][] labels = _paramLabels[tag.ordinal()];
                    final String label = labels[i1][0];
                    final String toolTip = labels[i1][1];
                    if (label != null) {
                        cw.setVisible(true);
                        cw.setText(label);
                        cw.setToolTip(toolTip);
                        if (target instanceof ConicTarget) {
                            String s = _getTargetParamValueAsString((ConicTarget) target, p);
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
    private String _getTargetParamValueAsString(ConicTarget target, Param param) {
        double d = _getTargetParamvalue(target, param);
        return String.valueOf(d);
    }
    // Return the current value of the given parameter
    private double _getTargetParamvalue(ConicTarget target, Param param) {
        switch (param) {
            case EPOCHOFEL:    return target.getEpoch().getValue();
            case ORBINC:       return target.getInclination().getValue();
            case LONGASCNODE:  return target.getANode().getValue();
            case LONGOFPERI:   return target.getPerihelion().getValue();
            case ARGOFPERI:    return target.getPerihelion().getValue();
            case MEANDIST:     return target.getAQ().getValue();
            case PERIDIST:     return target.getAQ().getValue();
            case ECCENTRICITY: return target.getE();
            case MEANLONG:     return target.getLM().getValue();
            case MEANANOM:     return target.getLM().getValue();
            case DAILYMOT:     return target.getN().getValue();
            case EPOCHOFPERI:  return target.getEpochOfPeri().getValue();
            default:           return 0;
        }
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
