// Copyright 2003
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ElevationPlotPanel.java 40560 2012-01-09 14:27:46Z swalker $

package jsky.plot;

import edu.gemini.spModel.core.Site;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.PrintableWithDialog;
import jsky.util.SaveableWithDialog;
import jsky.plot.util.gui.DateChooserDialog;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;
import jsky.util.gui.GridBagUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A panel for displaying an elevation plot for given target positions.
 *
 * @version $Revision: 40560 $
 * @author Allan Brighton
 */
public class ElevationPlotPanel extends JPanel implements ChangeListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ElevationPlotPanel.class);

    // Available time zone ids
    private static String[] _availableTimeZoneIds = {
        ElevationPlotModel.UT,
        ElevationPlotModel.LST,
        ElevationPlotModel.SITE_TIME
    };

    // Available time zone display names (corresponding to _availableTimeZones above)
    private static String[] _availableTimeZoneDisplayNames = {
        "UT",
        _I18N.getString("SiderealTime"),
        _I18N.getString("SiteTime")
    };

    // Plot Types
    private static final String ALTITUDE       = _I18N.getString("Altitude");
    private static final String PA             = _I18N.getString("ParallacticAngle");
    private static final String CONSTRAINTS    = _I18N.getString("Constraints");
    private static final String TIMING_WINDOWS = _I18N.getString("TimingWindows");

    // for saving and restoring user preferences
    private static final String _altitudePlotVisiblePrefName =
            ElevationPanel.class.getName() + ".altitudePlotVisible";

    private static final String _paPlotVisiblePrefName =
            ElevationPanel.class.getName() + ".paPlotVisible";

    private static final String _constraintsMarkerVisiblePrefName =
            ElevationPanel.class.getName() + ".constraintsMarkerVisible";

    private static final String _timingWindowsMarkerVisiblePrefName =
            ElevationPanel.class.getName() + ".timingWindowsMarkerVisible";

    // Displays the elevation plot
    private ElevationPanel _elevationPanel;

    // Displays the observation chart
    private ObservationPanel _observationPanel;

    // Displays a tabbed pane containing the data tables
    private TablePanel _tablePanel;

    // Tabbed pane containing the main windows
    private JTabbedPane _mainTabbedPane;

    // Used to select a date
    private DateChooserDialog _dateChooserDialog;

    // Provides the model data for the graph and tables
    private ElevationPlotModel _model;

    // The top level parent frame (or internal frame) used to close the window.
    private Component _parent;

    // The top level parent frame (or internal frame) of the target list dialog.
    private Component _targetListFrame;

    // True if this is the main application window
    private boolean _isMainWindow = false;

    // Array of buttons corresponding to the available sites
    private JRadioButton[] _siteButtons;

    // Array of buttons corresponding to the available time zones
    private JRadioButton[] _timeZoneButtons;

    // Panel holding the plot type buttons
    private JPanel _plotTypePanel;

    // Array of buttons corresponding to the available time zones
    private JCheckBox[] _plotTypeButtons;

    // Set to true while updating the GUI from the model
    private boolean _ignoreEvents = false;


    // Action for printing the graph
    private AbstractAction _printAction = new AbstractAction(_I18N.getString("print")) {
        public void actionPerformed(ActionEvent evt) {
            print();
        }
    };

    // Action for saving the graph to a file in PNG format
    private AbstractAction _saveAsAction = new AbstractAction(_I18N.getString("saveAs")) {
        public void actionPerformed(ActionEvent evt) {
            saveAs();
        }
    };

    // Action for closing the graph frame
    private AbstractAction _closeAction = new AbstractAction(_I18N.getString("close")) {
        public void actionPerformed(ActionEvent evt) {
            close();
        }
    };

    // Action for choosing the date
    private AbstractAction _dateAction = new AbstractAction(_I18N.getString("date")) {
        public void actionPerformed(ActionEvent evt) {
            selectDate();
        }
    };

    /**
     * Create an elevation plot panel.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     */
    public ElevationPlotPanel(Component parent) {
        _parent = parent;
        Preferences.manageSize(this, new Dimension(800, 400), getClass().getName() + ".size");

        _elevationPanel = new ElevationPanel();
        _observationPanel = new ObservationPanel();
        _tablePanel = new TablePanel();

        _mainTabbedPane = new JTabbedPane();
        _mainTabbedPane.add(_elevationPanel, "Elevation Plot");
        _mainTabbedPane.add(_observationPanel, "Observation Chart");
        _mainTabbedPane.add(_tablePanel, "Tables");
        _mainTabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updatePlotOptions();
            }
        });

        GridBagUtil layout = new GridBagUtil(this);
        layout.add(_mainTabbedPane, 0, 0, 1, 1, 1.0, 1.0,
                   GridBagConstraints.BOTH,
                   GridBagConstraints.CENTER,
                   new Insets(0, 0, 0, 0));
        layout.add(_makeButtonPanel(), 0, 1, 1, 1, 1.0, 0.0,
                   GridBagConstraints.HORIZONTAL,
                   GridBagConstraints.SOUTH,
                   new Insets(6, 11, 6, 11));
    }

    /**
     * Set the model containing the graph data and update the display.
     */
    public void setModel(ElevationPlotModel model) {
        _model = model;
        _elevationPanel.setModel(model);
        _observationPanel.setModel(model);
        _tablePanel.setModel(model);

        _update();

        _model.removeChangeListener(this);
        _model.addChangeListener(this);
    }

    /** Called when the model changes */
    public void stateChanged(ChangeEvent e) {
        _update();
    }


    // Update the GUI from the model
    private void _update() {
        try {
            _ignoreEvents = true;

            String siteName = _model.getSite().mountain;
            for (int i = 0; i < Site.values().length; i++) {
                if (siteName.equals(Site.values()[i].mountain)) {
                    if (!_siteButtons[i].isSelected()) {
                        _siteButtons[i].setSelected(true);
                    }
                    break;
                }
            }

            String id = _model.getTimeZoneId();
            for (int i = 0; i < _availableTimeZoneIds.length; i++) {
                if (id.equals(_availableTimeZoneIds[i])) {
                    if (!_timeZoneButtons[i].isSelected()) {
                        _timeZoneButtons[i].setSelected(true);
                    }
                    break;
                }
            }
        } finally {
            _ignoreEvents = false;
        }
    }

    /**
     * Return the model containing the graph data.
     */
    public ElevationPlotModel getModel() {
        return _model;
    }


    // Create and return a button panel with site and time options.
    private JPanel _makeButtonPanel() {
        JPanel panel = new JPanel();
        GridBagUtil layout = new GridBagUtil(panel);
        layout.add(_makeSitePanel(), 0, 0, 1, 1, 1.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.WEST,
                   new Insets(0, 0, 0, 0));
        layout.add(_makePlotTypePanel(), 1, 0, 1, 1, 1.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.CENTER,
                   new Insets(0, 0, 0, 0));
        layout.add(_makeTimePanel(), 2, 0, 1, 1, 1.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.EAST,
                   new Insets(0, 0, 0, 0));

        return panel;
    }

    // Create and return the "Site" button panel.
    private JPanel _makeSitePanel() {
        JPanel panel = new JPanel();
        Site[] sites = Site.values();
        ButtonGroup group = new ButtonGroup();
        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JRadioButton rb = (JRadioButton) e.getSource();
                if (!_ignoreEvents && rb.isSelected())
                    setSite(rb.getText());
            }
        };
        JLabel label = new JLabel("Site:");
        GridBagUtil layout = new GridBagUtil(panel);
        layout.add(label, 0, 0, 1, 1, 0.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.WEST,
                   new Insets(0, 0, 0, 0));
        _siteButtons = new JRadioButton[sites.length];
        for (int i = 0; i < sites.length; i++) {
            _siteButtons[i] = new JRadioButton(sites[i].mountain, i == 0);
            layout.add(_siteButtons[i], i+1, 0, 1, 1, 0.0, 0.0,
                       GridBagConstraints.NONE,
                       GridBagConstraints.WEST,
                       new Insets(0, 3, 0, 0));
            group.add(_siteButtons[i]);
            _siteButtons[i].addItemListener(itemListener);
        }
        return panel;
    }

    // Create and return the plot type button panel.
    private JPanel _makePlotTypePanel() {
        _plotTypePanel = new JPanel();
        String[] types = new String[] {ALTITUDE, PA, CONSTRAINTS, TIMING_WINDOWS};
        boolean[] selected = new boolean[] {
            Preferences.get(_altitudePlotVisiblePrefName, true),
            Preferences.get(_paPlotVisiblePrefName, false),
            Preferences.get(_constraintsMarkerVisiblePrefName, false),
            Preferences.get(_timingWindowsMarkerVisiblePrefName, false)
        };
        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JCheckBox cb = (JCheckBox) e.getSource();
                if (!_ignoreEvents)
                    setPlotVisible(cb.getText(), cb.isSelected());
            };
        };
        JLabel label = new JLabel("Plot:");
        GridBagUtil layout = new GridBagUtil(_plotTypePanel);
        layout.add(label, 0, 0, 1, 1, 0.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.WEST,
                   new Insets(0, 0, 0, 0));
        _plotTypeButtons = new JCheckBox[types.length];
        for (int i = 0; i < types.length; i++) {
            _plotTypeButtons[i] = new JCheckBox(types[i], selected[i]);
            layout.add(_plotTypeButtons[i], i+1, 0, 1, 1, 0.0, 0.0,
                       GridBagConstraints.NONE,
                       GridBagConstraints.WEST,
                       new Insets(0, 3, 0, 0));
            _plotTypeButtons[i].addItemListener(itemListener);
            setPlotVisible(types[i], selected[i]);
        }
        return _plotTypePanel;
    }

    // Create and return the "Time" button panel.
    private JPanel _makeTimePanel() {
        JPanel panel = new JPanel();
        _timeZoneButtons = new JRadioButton[_availableTimeZoneDisplayNames.length];
        ButtonGroup group = new ButtonGroup();
        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JRadioButton rb = (JRadioButton) e.getSource();
                if (!_ignoreEvents && rb.isSelected()) {
                    String s = rb.getText();
                    for (int i = 0; i < _availableTimeZoneDisplayNames.length; i++) {
                        if (_availableTimeZoneDisplayNames[i].equals(s)) {
                            setTimeZone(_availableTimeZoneDisplayNames[i], _availableTimeZoneIds[i]);
                            break;
                        }
                    }
                }
            };
        };
        JLabel label = new JLabel("Time:");
        GridBagUtil layout = new GridBagUtil(panel);
        layout.add(label, 0, 0, 1, 1, 0.0, 0.0,
                   GridBagConstraints.NONE,
                   GridBagConstraints.WEST,
                   new Insets(0, 0, 0, 0));
        for (int i = 0; i < _availableTimeZoneDisplayNames.length; i++) {
            _timeZoneButtons[i] = new JRadioButton(_availableTimeZoneDisplayNames[i], i == 0);
            layout.add(_timeZoneButtons[i], i+1, 0, 1, 1, 0.0, 0.0,
                       GridBagConstraints.NONE,
                       GridBagConstraints.WEST,
                       new Insets(0, 3, 0, 0));
            group.add(_timeZoneButtons[i]);
            _timeZoneButtons[i].addItemListener(itemListener);
        }

        return panel;
    }


    /** Set the time zone to display the X axis values */
    public void setTimeZone(String timeZoneDisplayName, String timeZoneId) {
        _model.setTimeZone(timeZoneDisplayName, timeZoneId);
    }


    /** Set the telescope site by name */
    private void setSite(String name) {
        for (Site s : Site.values()) {
            if (s.mountain.equals(name)) {
                _model.setSite(s);
                break;
            }
        }
    }

    /** Set the plot type to "Atltitude" or "Parallactic Angle" */
    public void setPlotVisible(String name, boolean visible) {
        if (name.equals(ALTITUDE)) {
            _elevationPanel.setAltitudePlotVisible(visible);
            Preferences.set(_altitudePlotVisiblePrefName, visible);
        } else if (name.equals(PA)) {
            _elevationPanel.setPaPlotVisible(visible);
            Preferences.set(_paPlotVisiblePrefName, visible);
            _elevationPanel.setY2AxisLabel(ElevationPanel.Y2_AXIS_PA);
            if (visible) {
                _elevationPanel.setY2AxisLabel(ElevationPanel.Y2_AXIS_PA);
            } else {
                _elevationPanel.setY2AxisLabel(ElevationPanel.Y2_AXIS_AIRMASS);
            }
        } else if (name.endsWith(CONSTRAINTS)) {
            _elevationPanel.setElevationConstraintsMarkerVisible(visible);
            Preferences.set(_constraintsMarkerVisiblePrefName, visible);

        } else if (name.equals(TIMING_WINDOWS)) {
            _elevationPanel.setTimingWindowsMarkerVisible(visible);
            Preferences.set(_timingWindowsMarkerVisiblePrefName, visible);
        }
    }

    // Return the dialog used to select a date
    private DateChooserDialog getDateChooserDialog(Calendar cal, boolean controlPane) {
        if (_dateChooserDialog == null)
            _dateChooserDialog = new DateChooserDialog(SwingUtil.getFrame(this), cal, controlPane);
        return _dateChooserDialog;
    }


    /** Display a dialog for printing the graph */
    public void print() {
        Component c = _mainTabbedPane.getSelectedComponent();
        if (c instanceof PrintableWithDialog) {
            try {
                ((PrintableWithDialog) c).print();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }

    /**
     * Display a dialog for saving the currently selected component to a file.
     * For graphs and charts, the format is PNG. For tables, ?
     */
    public void saveAs() {
        Component c = _mainTabbedPane.getSelectedComponent();
        if (c instanceof SaveableWithDialog) {
            try {
                ((SaveableWithDialog) c).saveAs();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }


    /** Return true if this is the main application window */
    public boolean isMainWindow() {
        return _isMainWindow;
    }

    /** Set to true if this is the main application window */
    public void setIsMainWindow(boolean b) {
        _isMainWindow = b;
    }

    /** Return the top level parent frame (or internal frame) containing this panel. */
    public Component getParentFrame() {
        return _parent;
    }

    /** Return the menubar of this panel's parent frame (for customization) */
    public ElevationPlotMenuBar getMenuBar() {
        if (_parent instanceof JFrame) {
            return (ElevationPlotMenuBar) (((JFrame) _parent).getJMenuBar());
        }
        if (_parent instanceof JInternalFrame) {
            return (ElevationPlotMenuBar) (((JInternalFrame) _parent).getJMenuBar());
        }
        return null;
    }

    /** Close this window's frame */
    public void close() {
        if (isMainWindow()) {
            System.exit(0);
        } else {
            if (_parent != null)
                _parent.setVisible(false);
        }
    }

    /** Dispose of the parent frame */
    public void dispose() {
        if (_parent instanceof JFrame) {
            ((JFrame) _parent).dispose();
        } else if (_parent instanceof JInternalFrame) {
            ((JInternalFrame) _parent).dispose();
        }
    }


    /** Display a dialog for selecting the date */
    public void selectDate() {
        Calendar cal = Calendar.getInstance(_model.getTimeZone());
        cal.setTime(_model.getDate());
        DateChooserDialog dialog = getDateChooserDialog(cal, true);
        dialog.show();
        if (!dialog.isCanceled()) {
            Date date = dialog.getDate();
            _model.setDate(date);
        }
    }

    /** Return the Action for printing the graph */
    public AbstractAction getPrintAction() {
        return _printAction;
    }

    /** Return the Action for saving the graph to a file in PNG format */
    public AbstractAction getSaveAsAction() {
        return _saveAsAction;
    }

    /** Return the Action for closing the graph frame */
    public AbstractAction getCloseAction() {
        return _closeAction;
    }

    /** Return the Action for selecting the date */
    public AbstractAction getDateAction() {
        return _dateAction;
    }

    // Return the elevation plot
    public ElevationPanel getElevationPanel() {
        return _elevationPanel;
    }

    // Return the observation chart
    public ObservationPanel getObservationPanel() {
        return _observationPanel;
    }

    /** Set the visibility of the graph and chart legends */
    public void setShowLegend(boolean show) {
        _elevationPanel.setShowLegend(show);
        _observationPanel.setShowLegend(show);
    }

    /**
      * Update the visibility of the plot option buttons based on the current settings.
     */
    public void updatePlotOptions() {
        if (_plotTypeButtons != null) {
            _plotTypePanel.setVisible( _mainTabbedPane.getSelectedComponent() == _elevationPanel);
        }
    }
}


