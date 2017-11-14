package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
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
import java.awt.event.ItemListener;
import java.io.Serializable;

import java.util.Hashtable;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;


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

    // Colors for color coding.
    private enum ColorManager {
        instance;

        private final static Paint[] COLORS = ChartColor.createDefaultPaintArray();
        private int idx = 0;

        public void reset() {
            idx = 0;
        }

        public Paint nextColor() {
            final Paint p = COLORS[idx % COLORS.length];
            ++idx;
            return p;
        }
    }

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
    public void setSelectedObservations(final ISPObservation[] obs) {
        _selectedObservations = obs;

        // If the plot is already being displayed, update the custom legend, if needed
        if (_elevationPanel != null) {
            _elevationPanel.setLegendItems(getLegendItems());
        }
    }

    /** Called when the ElevationPlot frame is first created */
    @Override
    public void stateChanged(final ChangeEvent e) {
        if (_elevationPanel == null) {
            _elevationPanel = ElevationPlotManager.get().getElevationPanel();
            _addMenuItems(ElevationPlotManager.get().getMenuBar());
        }
    }


    // Add the OT specific menu items
    private void _addMenuItems(final ElevationPlotMenuBar menuBar) {
        final JMenu menu = menuBar.getViewMenu();
        menu.add(_createViewLabelMenu());
        menu.add(_createViewColorCodeMenu());
    }

    private JMenu createRadioButtonMenuFromStrings(final String menuName, final String[] menuItems,
                                                   final String property, final Consumer<String> setter) {
        final JMenu menu = new JMenu(menuName);
        final ButtonGroup group = new ButtonGroup();

        final ItemListener listener = e -> {
            final JRadioButtonMenuItem b = (JRadioButtonMenuItem) e.getSource();
            if (b.isSelected())
                setter.accept(b.getText());
        };

        for (final String s: menuItems) {
            final JRadioButtonMenuItem b = new JRadioButtonMenuItem(s);
            _buttonMap.put(s, b);
            if (property.equals(s))
                b.setSelected(true);
            menu.add(b);
            group.add(b);
            b.addItemListener(listener);
        }

        return menu;
    }

    // Create the "Label trajectory with" menu
    private JMenu _createViewLabelMenu() {
        boolean showLabels = !_labelTrajectory.equals(_LABEL_NONE);
        _elevationPanel.setShowTrajectoryLabels(showLabels);
        return createRadioButtonMenuFromStrings("Label Trajectory with", _LABEL_OPTIONS, _labelTrajectory, this::_setLabelTrajectory);
    }


    // Set the label trajectory to one of the _LABEL_OPTIONS
    private void _setLabelTrajectory(String label) {
        _labelTrajectory = label;
        Preferences.set(_labelTrajectoryPrefName, _labelTrajectory);
        boolean showLabels = !_labelTrajectory.equals(_LABEL_NONE);
        _elevationPanel.setShowTrajectoryLabels(showLabels);
        _fireChangeEvent();
    }


    // Return true if the legend should display the target name
    public boolean useTargetName() {
        return _labelTrajectory.equals(_LABEL_TARGET_NAME);
    }


    // Create the "Color Code Trajectories" menu
    private JMenu _createViewColorCodeMenu() {
        _elevationPanel.setLegendItems(getLegendItems());
        return createRadioButtonMenuFromStrings("Color Code Trajectories", _COLOR_CODE_OPTIONS, _colorCodeTrajectories, this::_setColorCode);
    }

    // Set the color code trajectories to one of the _COLOR_CODE_OPTIONS
    private void _setColorCode(String label) {
        _colorCodeTrajectories = label;
        Preferences.set(_colorCodeTrajectoriesPrefName, _colorCodeTrajectories);
        _elevationPanel.setLegendItems(getLegendItems());
        _fireChangeEvent();
    }


    // Create a LegendItemCollection by:
    // 1. Executing an optional preprocessing function (to precalculate some colors and create some default legend items); and
    // 2. Executing an extractor for each selected observation, which produces an optional ID. If the ID is defined, create
    //    a new legend item for it.
    // We also ensure that IDs for LegendItems are unique so that we don't, for example, end up with multiple entries for
    // "Band 4" if observations are labeled by band, or repeated priorities if observations are labeled by priority.
    private LegendItemCollection createLegendItemsPreOpt(final Option<Function<TreeMap<String, Paint>, LegendItemCollection>> pref,
                                                         final Function<ISPObservation, Option<String>> extractor) {
        final Paint[] colors = new Paint[_selectedObservations.length];
        final TreeMap<String,Paint> paintMap = new TreeMap<>();
        final Set<String> licIds = new TreeSet<>();
        final LegendItemCollection lic = new LegendItemCollection();

        // Perform the preprocessing function, if it exists.
        pref.foreach(f -> lic.addAll(f.apply(paintMap)));
        for (int i=0 ; i < lic.getItemCount(); ++i) {
            final LegendItem li = lic.get(i);
            licIds.add(li.getLabel());
        }

        // Iterate over the selected observations.
        for (int obsIdx = 0; obsIdx < _selectedObservations.length; ++obsIdx) {
            final ISPObservation obs = _selectedObservations[obsIdx];
            final Option<String> idOpt = extractor.apply(obs);
            final Paint p = idOpt.map(id -> paintMap.computeIfAbsent(id, s -> ColorManager.instance.nextColor())).getOrElse(Color.BLACK);
            colors[obsIdx] = p;
            idOpt.foreach(id -> {
                if (!licIds.contains(id)) {
                    final LegendItem li = new LegendItem(id, p);
                    lic.add(li);
                    licIds.add(id);
                }
            });
        }

        _elevationPanel.setItemColors(colors);
        return lic;
    }

    // Convenience methods to call createLegendItemsPreOpt without preprocessing / without options.
    private LegendItemCollection createLegendItemsPre(final Function<TreeMap<String, Paint>, LegendItemCollection> pref,
                                                      final Function<ISPObservation, String> extractor) {
        return createLegendItemsPreOpt(new Some<>(pref), obs -> new Some<>(extractor.apply(obs)));
    }

    private LegendItemCollection createLegendItemsOpt(final Function<ISPObservation, Option<String>> extractor) {
        return createLegendItemsPreOpt(None.instance(), extractor);
    }

    private LegendItemCollection createLegendItems(final Function<ISPObservation, String> extractor) {
        return createLegendItemsPreOpt(None.instance(), obs -> new Some<>(extractor.apply(obs)));
    }

    // Get all the legend items.
    // Except for the special case of no color code, delegates all of the work to the createLegendItems... functions.
    private LegendItemCollection getLegendItems() {
        final ColorManager colorManager = ColorManager.instance;
        colorManager.reset();

        final LegendItemCollection lic;
        switch (_colorCodeTrajectories) {
            case _COLOR_CODE_OBS:
                lic = createLegendItems(obs -> obs.getObservationIDAsString("unknown"));
                break;

            case _COLOR_CODE_PROG:
                lic = createLegendItemsOpt(obs -> ImOption.apply(obs.getProgramID()).map(SPProgramID::stringValue));
                break;

            case _COLOR_CODE_BAND:
                lic = createLegendItemsPre(tm -> {
                    final LegendItemCollection plic = new LegendItemCollection();
                    for (int i=0; i < 5; ++i) {
                        final String band = (i == 0) ? "Default Band" : String.format("Band %d", i);
                        final Paint p = (i == 0) ? Color.BLACK : colorManager.nextColor();
                        tm.put(band, p);
                        plic.add(new LegendItem(band, p));
                    }
                    return plic;
                  }, obs -> {
                    final SPProgram prog = (SPProgram) SPDB.get().lookupProgram(obs.getProgramKey()).getDataObject();
                    final String bandStr = prog.getQueueBand();
                    return (bandStr == null || bandStr.isEmpty()) ? "Default Band" : String.format("Band %s", bandStr);
                });
                break;

            case _COLOR_CODE_PRIORITY:
                lic = createLegendItemsPre(tm -> {
                    final LegendItemCollection plic = new LegendItemCollection();
                    for (final SPObservation.Priority prio: _PRIORITIES) {
                        final Paint p = colorManager.nextColor();
                        final String prioStr = String.format("%s Priority", prio.displayValue());
                        tm.put(prioStr, p);
                        plic.add(new LegendItem(prioStr, p));
                    }
                    return plic;
                }, obs -> String.format("%s Priority", ((SPObservation) obs.getDataObject()).getPriority().displayValue()));
                break;

            case _COLOR_CODE_NONE:
                // Special case: no legend, and all lines are drawn in black.
                lic = new LegendItemCollection();
                final Paint[] colors = new Paint[_selectedObservations.length];
                java.util.Arrays.fill(colors, Color.BLACK);
                _elevationPanel.setItemColors(colors);
                break;

            default:
                // Shouldn't ever get here.
                lic = null;
                _elevationPanel.setItemColors(null);
        }

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

