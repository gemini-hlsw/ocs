package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.SPObservation;
import jsky.plot.*;
import jsky.util.Preferences;
import jsky.util.Storeable;
import org.jfree.chart.ChartColor;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;

import java.util.Hashtable;
import java.util.TreeMap;


/** Adds some OT specific features to the elevation plot panel */
public class SPElevationPlotPlugin implements ChangeListener, Storeable {

    // Singleton instance of this class
    private static SPElevationPlotPlugin _instance;

    private final String _className = getClass().getName();
    private final String _labelTrajectoryPrefName = _className + ".labelTrajectory";
    private final String _colorCodeTrajectoriesPrefName = _className + ".colorCodeTrajectories";

    // Options for "Label Trajectory with" menu item
    private static final String _LABEL_OBS_ID = "Observation ID";
    private static final String _LABEL_TARGET_NAME = "Target Name";
    private static final String _LABEL_NONE = "None";

    private static final String[] _LABEL_OPTIONS = {
        _LABEL_OBS_ID, _LABEL_TARGET_NAME, _LABEL_NONE
    };

    // Set to one of the above values
    private String _labelTrajectory;

    // Options for "Color code trajectories" menu item
    private static final String _COLOR_CODE_OBS = "Each Observation";
    private static final String _COLOR_CODE_PROG = "Each Program";
    private static final String _COLOR_CODE_BAND = "Each Band";
    private static final String _COLOR_CODE_PRIORITY = "Each User Priority";
    private static final String _COLOR_CODE_NONE = "None";

    private static final String[] _COLOR_CODE_OPTIONS = {
        _COLOR_CODE_OBS, _COLOR_CODE_PROG, _COLOR_CODE_BAND,
        _COLOR_CODE_PRIORITY, _COLOR_CODE_NONE
    };


    private static final SPObservation.Priority[] _PRIORITIES = new SPObservation.Priority[]{
            SPObservation.Priority.HIGH,
            SPObservation.Priority.MEDIUM,
            SPObservation.Priority.LOW,
    };


    // Set to one of the above values
    private String _colorCodeTrajectories;

    // Array of colors for color coding
    private static final Paint[] _COLORS = ChartColor.createDefaultPaintArray();

    // listener for change events
    private ChangeListener _changeListener;

    // Array of observations being displayed by the elevation plot (OT specific information)
    private ISPObservation[] _selectedObservations;

    // Reference to the elevation plot panel
    private ElevationPanel _elevationPanel;

    // Used to select a radio button menu item, given the label
    private Hashtable<String, JRadioButtonMenuItem> _buttonMap = new Hashtable<>();


    // private constructor
    private SPElevationPlotPlugin() {
        _labelTrajectory = Preferences.get(_labelTrajectoryPrefName, _LABEL_OBS_ID);
        _colorCodeTrajectories = Preferences.get(_colorCodeTrajectoriesPrefName, _COLOR_CODE_OBS);
    }

    /** Return the singleton instance of this class */
    public static SPElevationPlotPlugin getInstance() {
        if (_instance == null)
            _instance = new SPElevationPlotPlugin();
        return _instance;

    }

    /** Set the array of observations being displayed by the elevation plot (OT specific information) */
    public void setSelectedObservations(ISPObservation[] obs) {
        _selectedObservations = obs;

        // If the plot is already being displayed, update the custom legend, if needed
        if (_elevationPanel != null) {
            _elevationPanel.setLegendItems(getLegendItems());
        }
    }

    /** Called when the ElevationPlot frame is first created */
    @Override
    public void stateChanged(ChangeEvent e) {
        if (_elevationPanel == null) {
            _elevationPanel = ElevationPlotManager.get().getElevationPanel();
            _addMenuItems(ElevationPlotManager.get().getMenuBar());
        }
    }


    // Add the OT specific menu items
    private void _addMenuItems(ElevationPlotMenuBar menuBar) {
        JMenu menu = menuBar.getViewMenu();

        menu.add(_createViewLabelMenu());
        menu.add(_createViewColorCodeMenu());
    }

    // Create the "Label trajectory with" menu
    private JMenu _createViewLabelMenu() {
        JMenu menu = new JMenu("Label Trajectory with");
        JRadioButtonMenuItem[] b = new JRadioButtonMenuItem[_LABEL_OPTIONS.length];
        ButtonGroup group = new ButtonGroup();

        final ItemListener itemListener = e -> {
            final JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
            if (rb.isSelected()) {
                _setLabelTrajectory(rb.getText());
            }
        };

        boolean showLabels = !_labelTrajectory.equals(_LABEL_NONE);
        _elevationPanel.setShowTrajectoryLabels(showLabels);

        for (int i = 0; i < _LABEL_OPTIONS.length; i++) {
            b[i] = new JRadioButtonMenuItem(_LABEL_OPTIONS[i]);
            _buttonMap.put(_LABEL_OPTIONS[i], b[i]);
            if (_labelTrajectory.equals(_LABEL_OPTIONS[i]))
                b[i].setSelected(true);
            menu.add(b[i]);
            group.add(b[i]);
            b[i].addItemListener(itemListener);
        }

        return menu;
    }


    // Set the label trajectory to one of the _LABEL_OPTIONS
    private void _setLabelTrajectory(String label) {
        _labelTrajectory = label;
        Preferences.set(_labelTrajectoryPrefName, _labelTrajectory);
        boolean showLabels = !_labelTrajectory.equals(_LABEL_NONE);
        _elevationPanel.setShowTrajectoryLabels(showLabels);
        _fireChangeEvent();
    }


    /** Return true if the legend should display the target name */
    public boolean useTargetName() {
        return _labelTrajectory.equals(_LABEL_TARGET_NAME);
    }

    /** Return true if the legend should display the observation id */
    public boolean useObsId() {
        return _labelTrajectory.equals(_LABEL_OBS_ID);
    }


    // Create the "Color Code Trajectories" menu
    private JMenu _createViewColorCodeMenu() {
        JMenu menu = new JMenu("Color Code Trajectories");
        JRadioButtonMenuItem[] b = new JRadioButtonMenuItem[_COLOR_CODE_OPTIONS.length];
        ButtonGroup group = new ButtonGroup();

        final ItemListener itemListener = e -> {
            JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
            if (rb.isSelected()) {
                _setColorCode(rb.getText());
            }
        };

        _elevationPanel.setLegendItems(getLegendItems());

        for (int i = 0; i < _COLOR_CODE_OPTIONS.length; i++) {
            b[i] = new JRadioButtonMenuItem(_COLOR_CODE_OPTIONS[i]);
            _buttonMap.put(_COLOR_CODE_OPTIONS[i], b[i]);
            if (_colorCodeTrajectories.equals(_COLOR_CODE_OPTIONS[i]))
                b[i].setSelected(true);
            menu.add(b[i]);
            group.add(b[i]);
            b[i].addItemListener(itemListener);
        }

        return menu;
    }

    // Set the color code trajectories to one of the _COLOR_CODE_OPTIONS
    private void _setColorCode(String label) {
        _colorCodeTrajectories = label;
        Preferences.set(_colorCodeTrajectoriesPrefName, _colorCodeTrajectories);
        _elevationPanel.setLegendItems(getLegendItems());
        _fireChangeEvent();
    }


    /**  Return a custom LegendItemCollection based on the current settings  and set the item colors to match */
    public LegendItemCollection getLegendItems() {
        int colorIndex = 0;
        Paint[] colors = new Paint[_selectedObservations.length];
        final TreeMap<String,Paint> paintMap = new TreeMap<>();
        LegendItemCollection lic = new LegendItemCollection();

        switch (_colorCodeTrajectories) {
            case _COLOR_CODE_OBS:
                for (int i = 0; i < _selectedObservations.length; i++) {
                    String obsId = _selectedObservations[i].getObservationIDAsString("unknown");
                    if ((colors[i] = paintMap.get(obsId)) == null) {
                        Paint color = _COLORS[colorIndex++ % _COLORS.length];
                        paintMap.put(obsId, color);
                        colors[i] = color;
                        lic.add(new LegendItem(obsId, color));
                    }
                }
                break;

            case _COLOR_CODE_PROG:
                for (int i = 0; i < _selectedObservations.length; i++) {
                    SPProgramID spProgId = _selectedObservations[i].getProgramID();
                    if (spProgId != null) {
                        String progId = spProgId.stringValue();
                        if ((colors[i] = paintMap.get(progId)) == null) {
                            Paint color = _COLORS[colorIndex++ % _COLORS.length];
                            paintMap.put(progId, color);
                            colors[i] = color;
                            lic.add(new LegendItem(progId, color));
                        }
                    }
                }
                break;

            case _COLOR_CODE_BAND:
                // get the order of items right
                Paint color = Color.black;
                String queueBand = "Default Band";
                paintMap.put(queueBand, color);
                lic.add(new LegendItem(queueBand, color));
                for (int i = 1; i < 5; i++) {
                    color = _COLORS[colorIndex++ % _COLORS.length];
                    queueBand = "Band " + i;
                    paintMap.put(queueBand, color);
                    lic.add(new LegendItem(queueBand, color));
                }
                IDBDatabaseService db = SPDB.get();
                for (int i = 0; i < _selectedObservations.length; i++) {
                    SPNodeKey progKey = _selectedObservations[i].getProgramKey();
                    ISPProgram prog = db.lookupProgram(progKey);
                    // LORD OF DESTRUCTION: DataObjectManager get without set
                    SPProgram spProg = (SPProgram) prog.getDataObject();
                    queueBand = spProg.getQueueBand();
                    if (queueBand == null || queueBand.length() == 0)
                        queueBand = "Default Band";
                    else
                        queueBand = "Band " + queueBand;
                    if ((colors[i] = paintMap.get(queueBand)) == null) {
                        color = _COLORS[colorIndex++ % _COLORS.length];
                        paintMap.put(queueBand, color);
                        colors[i] = color;
                        lic.add(new LegendItem(queueBand, color));
                    }
                }
                break;

            case _COLOR_CODE_PRIORITY:
                // get the order of items right
                for (SPObservation.Priority pr: _PRIORITIES) {
                    final Paint c = _COLORS[colorIndex++ % _COLORS.length];
                    paintMap.put(pr.displayValue(), c);
                    String label = pr.displayValue() + " Priority";
                    lic.add(new LegendItem(label, c));
                }
                for (int i = 0; i < _selectedObservations.length; i++) {
                    // LORD OF DESTRUCTION: DataObjectManager get without set
                    final SPObservation spObs = (SPObservation) _selectedObservations[i].getDataObject();
                    final SPObservation.Priority prio = spObs.getPriority();
                    if ((colors[i] = paintMap.get(prio.displayValue())) == null) {
                        final Paint c = _COLORS[colorIndex++ % _COLORS.length];
                        paintMap.put(prio.displayValue(), c);
                        colors[i] = c;
                        String label = prio.displayValue() + " Priority";
                        lic.add(new LegendItem(label, c));
                    }
                }
                break;

            case _COLOR_CODE_NONE:
                for (int i = 0; i < _selectedObservations.length; i++) {
                    colors[i] = Color.black;
                }
                break;

            default:
                // shouldn't ever get here
                _elevationPanel.setItemColors(null);
                return null;
        }

        _elevationPanel.setItemColors(colors);
        return lic;
    }


    /**
     * Register to receive change events from this object whenever a menu selection is made.
     * This is used to redisplay the same elevation plot with the new settings.
     */
    public void setChangeListener(ChangeListener l) {
        _changeListener = l;
    }

    private void _fireChangeEvent() {
        if (_changeListener != null)
            _changeListener.stateChanged(new ChangeEvent(this));
    }


    // -- Implement the Storeable interface -- */

    /** Store the current settings in a serializable object and return the object. */
    @Override
    public Object storeSettings() {
        ElevationPlotPanel plotPanel = ElevationPlotManager.get();
        if (plotPanel != null) {
            JCheckBoxMenuItem showLegendButton = plotPanel.getMenuBar().getShowLegendMenuItem();
            ElevationPlotModel model = plotPanel.getModel();
            return new PlotSettings(_labelTrajectory, _colorCodeTrajectories, showLegendButton.isSelected(),
                                    model.getSite().mountain, model.getTimeZoneDisplayName(),
                                    model.getTimeZoneId());
        }
        return null;
    }

    /** Restore the settings previously stored. */
    @Override
    public boolean restoreSettings(Object obj) {
        if (!(obj instanceof PlotSettings))
            return false;

        PlotSettings ps = (PlotSettings) obj;

        AbstractButton b = _buttonMap.get(ps.labelTrajectory);
        if (b != null && !b.isSelected())
            b.setSelected(true);

        b = _buttonMap.get(ps.colorCodeTrajectories);
        if (b != null && !b.isSelected())
            b.setSelected(true);

        ElevationPlotPanel plotPanel = ElevationPlotManager.get();
        if (plotPanel != null) {
            b = plotPanel.getMenuBar().getShowLegendMenuItem();
            b.setSelected(ps.showLegend);

            ElevationPlotModel model = plotPanel.getModel();
            model.setSite(Site.tryParse(ps.site));
            model.setTimeZone(ps.timeZoneDisplayName, ps.timeZoneId);
        }

        return true;
    }

    // Local class used to store/restore plot settings
    private static class PlotSettings implements Serializable {
        String labelTrajectory;
        String colorCodeTrajectories;
        boolean showLegend;
        String site;
        String timeZoneDisplayName;
        String timeZoneId;

        PlotSettings(String label, String colorCode, boolean show, String siteName, String tzName, String tzId) {
            labelTrajectory = label;
            colorCodeTrajectories = colorCode;
            showLegend = show;
            site = siteName;
            timeZoneDisplayName = tzName;
            timeZoneId = tzId;
        }
    }
}

