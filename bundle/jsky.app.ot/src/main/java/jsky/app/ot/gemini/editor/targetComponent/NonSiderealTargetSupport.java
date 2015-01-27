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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for displaying and operating with non sidereal targets.
 * <p/>
 * Non sidereal targets can be {@link edu.gemini.spModel.target.system.ConicTarget} and
 * {@link edu.gemini.spModel.target.system.NamedTarget}
 *
 */
class NonSiderealTargetSupport {

    private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");

    // Groups together the widgets for one parameter for conic targets
    private static class ConicTargetParamWidgets {
        private final JLabel _label;
        private final NumberBoxWidget _entry;
        private final JLabel _units;

        public ConicTargetParamWidgets(JLabel label, NumberBoxWidget entry, JLabel units) {
            _label = label;
            _entry = entry;
            _units = units;
        }

        public NumberBoxWidget getEntry() {
            return _entry;
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
        public void setPos(TelescopeForm w, int row, int col) {
            col *= 3;
            row += 2;
            _updateConstraints(w, _label, row, col++);
            _updateConstraints(w, _entry, row, col++);
            _updateConstraints(w, _units, row, col);
        }

        private void _updateConstraints(TelescopeForm w, JComponent c, int row, int col) {
            GridBagLayout layout = (GridBagLayout) w.nonsiderealPW.getLayout();
            GridBagConstraints contraints = layout.getConstraints(c);
            contraints.gridx = col;
            contraints.gridy = row;
            layout.setConstraints(c, contraints);
        }
    }

    private enum Param {
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

    private static final class Labels {
        public final String label;
        public final String toolTip;
        public Labels(String label, String toolTip) {
            this.label = label;
            this.toolTip = toolTip;
        }
    }

    private static final Map<ITarget.Tag, Map<Param, Labels>> PARAM_LABELS;
    static {

        // Minor body param labels
        final Map<Param, Labels> mbps = new HashMap<>();
        mbps.put(Param.EPOCHOFEL,    new Labels("EPOCH", "Orbital Element Epoch"));
        mbps.put(Param.ORBINC,       new Labels("IN",    "Inclination"));
        mbps.put(Param.LONGASCNODE,  new Labels("OM",    "Longitude of Ascending Node"));
        mbps.put(Param.ARGOFPERI,    new Labels("W",     "Argument of Perihelion"));
        mbps.put(Param.PERIDIST,     new Labels("QR",    "Perihelion Distance"));
        mbps.put(Param.ECCENTRICITY, new Labels("EC",    "Eccentricity"));
        mbps.put(Param.EPOCHOFPERI,  new Labels("TP",    "Time of Perihelion Passage"));

        // Minor planet param labels
        final Map<Param, Labels> mpps = new HashMap<>();
        mpps.put(Param.EPOCHOFEL,    new Labels("EPOCH", "Orbital Element Epoch"));
        mpps.put(Param.ORBINC,       new Labels("IN",    "Inclination"));
        mpps.put(Param.LONGASCNODE,  new Labels("OM",    "Longitude of Ascending Node"));
        mpps.put(Param.ARGOFPERI,    new Labels("W",     "Argument of Perihelion"));
        mpps.put(Param.MEANDIST,     new Labels("A",     "Semi-major Axis"));
        mpps.put(Param.ECCENTRICITY, new Labels("EC",    "Eccentricity"));
        mpps.put(Param.MEANANOM,     new Labels("MA",    "Mean Anomaly"));

        // Label maps for each target type
        final HashMap<ITarget.Tag, Map<Param, Labels>> map = new HashMap<>();
        map.put(ITarget.Tag.NAMED,            Collections.<Param, Labels>emptyMap());
        map.put(ITarget.Tag.MPC_MINOR_PLANET, Collections.unmodifiableMap(mpps));
        map.put(ITarget.Tag.JPL_MINOR_BODY,   Collections.unmodifiableMap(mbps));

        // Done
        PARAM_LABELS = Collections.unmodifiableMap(map);

    }

    private static Map<Param, ConicTargetParamWidgets> mkConicWidgets(TelescopeForm f) {
        final Map<Param, ConicTargetParamWidgets> map = new HashMap<>();
        map.put(Param.EPOCHOFEL,    new ConicTargetParamWidgets(f.epochofelLabel,    f.epochofel,    f.epochofelUnits));
        map.put(Param.ORBINC,       new ConicTargetParamWidgets(f.orbincLabel,       f.orbinc,       f.orbincUnits));
        map.put(Param.LONGASCNODE,  new ConicTargetParamWidgets(f.longascnodeLabel,  f.longascnode,  f.longascnodeUnits));
        map.put(Param.LONGOFPERI,   new ConicTargetParamWidgets(f.longofperiLabel,   f.longofperi,   f.longofperiUnits));
        map.put(Param.ARGOFPERI,    new ConicTargetParamWidgets(f.argofperiLabel,    f.argofperi,    f.argofperiUnits));
        map.put(Param.MEANDIST,     new ConicTargetParamWidgets(f.meandistLabel,     f.meandist,     f.meandistUnits));
        map.put(Param.PERIDIST,     new ConicTargetParamWidgets(f.peridistLabel,     f.peridist,     f.peridistUnits));
        map.put(Param.ECCENTRICITY, new ConicTargetParamWidgets(f.eccentricityLabel, f.eccentricity, f.eccentricityUnits));
        map.put(Param.MEANLONG,     new ConicTargetParamWidgets(f.meanlongLabel,     f.meanlong,     f.meanlongUnits));
        map.put(Param.MEANANOM,     new ConicTargetParamWidgets(f.meananomLabel,     f.meananom,     f.meananomUnits));
        map.put(Param.DAILYMOT,     new ConicTargetParamWidgets(f.dailymotLabel,     f.dailymot,     f.dailymotUnits));
        map.put(Param.EPOCHOFPERI,  new ConicTargetParamWidgets(f.epochofperiLabel,  f.epochofperi,  f.epochofperiUnits));
        return Collections.unmodifiableMap(map);
    }

    private final TelescopeForm form;
    private final Map<Param, ConicTargetParamWidgets> widgets;
    private boolean ignoreResetCacheEvents = false;
    private SPTarget spTarget;

    NonSiderealTargetSupport(TelescopeForm w, SPTarget curPos) {
        form = w;
        spTarget = curPos;
        widgets = mkConicWidgets(w);
    }

    /** Add the given listener to all the entry widgets */
    public void initListeners(TextBoxWidgetWatcher watcher) {
        for (Param p: Param.values()) {
            widgets.get(p).getEntry().addWatcher(watcher);
        }
    }

    /**
     * Set the value of the given target field to whatever is contained in the given
     * widget. The widget is used to determine which field to set. This method is called
     * whenever the user types a value into one of the conic target entry widgets.
     */
    void setConicPos(ConicTarget target, NumberBoxWidget nbw) {
        try {
            double value = Double.valueOf(nbw.getValue());
            for (Map.Entry<Param, ConicTargetParamWidgets> e: widgets.entrySet()) {
                if (e.getValue().getEntry() == nbw) {
                    switch (e.getKey()) {
                        case EPOCHOFEL:    target.getEpoch().setValue(value);       break;
                        case ORBINC:       target.getInclination().setValue(value); break;
                        case LONGASCNODE:  target.getANode().setValue(value);       break;
                        case LONGOFPERI:   target.getPerihelion().setValue(value);  break;
                        case ARGOFPERI:    target.getPerihelion().setValue(value);  break;
                        case MEANDIST:     target.getAQ().setValue(value);          break;
                        case PERIDIST:     target.getAQ().setValue(value);          break;
                        case ECCENTRICITY: target.setE(value);                      break;
                        case MEANLONG:     target.getLM().setValue(value);          break;
                        case MEANANOM:     target.getLM().setValue(value);          break;
                        case DAILYMOT:     target.getN().setValue(value);           break;
                        case EPOCHOFPERI:  target.getEpochOfPeri().setValue(value); break;
                    }
                    break;
                }
            }
        } catch (NumberFormatException ex) {
            // do nothing (weak!)
        }
    }

    /**
     * Display the given target in the GUI
     * @param target the target position to display
     **/
    void showNonSiderealTarget(NonSiderealTarget target) {

        final ITarget.Tag tag = target.getTag();
        final Map<Param, Labels> labels = PARAM_LABELS.get(tag);

        // Update all the conic parameter widgets
        int row = 0;
        int col = 0;
        for (Param p: Param.values()) {
            final ConicTargetParamWidgets cw = widgets.get(p);
            final Labels labs = labels.get(p);
            if (labs != null) {
                cw.setVisible(true);
                cw.setText(labs.label);
                cw.setToolTip(labs.toolTip);
                if (target instanceof ConicTarget) {
                    Double d = _getTargetParamvalue((ConicTarget) target, p);
                    cw.setValue(String.valueOf(d));
                }
                cw.setPos(form, row, col++);
                if (col > 1) {
                    row++;
                    col = 0;
                }
            } else {
                cw.setVisible(false);
            }
        }

        // Update the RA and Dec
        form.xaxis.setText(target.getRaHms());
        form.yaxis.setText(target.getDecDms());

        // Update the valid-at date
        Date date = target.getDateForPosition();
        JTextField tf = (JTextField) form.calendarTime.getEditor().getEditorComponent();
        TimeDocument td = (TimeDocument)tf.getDocument();
        //if the date is null  use the current time
        if (date == null) {
            date = new Date();
        }
        form.calendarDate.setDate(date);
        td.setTime(timeFormatter.format(date));

        // Update the solar system stuff
        if (target instanceof NamedTarget) {
            form.planetsPanel.setVisible(true);
            NamedTarget pt = (NamedTarget) target;
            NamedTarget.SolarObject solarObject = pt.getSolarObject();
            if (solarObject != null) {
                int pos = solarObject.ordinal();
                if (pos < form.planetButtons.length) {
                    form.planetButtons[pos].setSelected(true);
                }
            }
        } else {
            form.planetsPanel.setVisible(false);
        }

        // And revalidate everything
        form.nonsiderealPW.revalidate();
        form.nonsiderealPW.repaint();

    }

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

    private final DropDownListBoxWidgetWatcher orbitalElementFormatWatcher =
        new DropDownListBoxWidgetWatcher()  {
            public void dropDownListBoxAction(DropDownListBoxWidget dd, int i, String val) {
                spTarget.setTargetType((ITarget.Tag) form.orbitalElementFormat.getSelectedItem());
                if (!ignoreResetCacheEvents) {
                    HorizonsService.resetCache();
                }
            }
        };

    void initOrbitalElementFormatChoices() {
        form.orbitalElementFormat.deleteWatcher(orbitalElementFormatWatcher);
        form.orbitalElementFormat.clear();
        form.orbitalElementFormat.addChoice(ITarget.Tag.JPL_MINOR_BODY);
        form.orbitalElementFormat.addChoice(ITarget.Tag.MPC_MINOR_PLANET);
        form.orbitalElementFormat.addChoice(ITarget.Tag.NAMED);
        form.orbitalElementFormat.addWatcher(orbitalElementFormatWatcher);
    }

    void showOrbitalElementFormat() {
        form.orbitalElementFormat.deleteWatcher(orbitalElementFormatWatcher);
        form.orbitalElementFormat.setSelectedItem(spTarget.getTarget().getTag());
        form.orbitalElementFormat.addWatcher(orbitalElementFormatWatcher);
    }

    void updatePos(SPTarget target) {
        spTarget = target;
    }

    void ignoreResetCacheEvents(boolean b) {
        ignoreResetCacheEvents = b;
    }

}
