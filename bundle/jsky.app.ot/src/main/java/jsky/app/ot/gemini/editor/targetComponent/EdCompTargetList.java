// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: EdCompTargetList.java 13566 2008-09-29 16:03:36Z swalker $
//
package jsky.app.ot.gemini.editor.targetComponent;

import com.jgoodies.forms.layout.CellConstraints;
import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.horizons.api.EphemerisEntry;
import edu.gemini.horizons.api.HorizonsQuery;
import edu.gemini.horizons.api.HorizonsReply;
import edu.gemini.horizons.api.OrbitalElements;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.gui.ButtonFlattener;
import edu.gemini.shared.gui.RotatedButtonUI;
import edu.gemini.shared.gui.ThinBorder;
import edu.gemini.shared.gui.calendar.JCalendarPopup;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.system.*;
import jsky.app.ot.OTOptions;
import jsky.app.ot.ags.*;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.gemini.editor.horizons.HorizonsPlotter;
import jsky.app.ot.gemini.editor.horizons.HorizonsService;
import jsky.app.ot.tpe.AgsClient;
import jsky.app.ot.tpe.GuideStarSupport;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.app.ot.ui.util.TimeDocument;
import jsky.app.ot.util.AuthSwingWorker;
import jsky.app.ot.util.Resources;
import jsky.catalog.*;
import jsky.catalog.skycat.FullMimeSimbadCatalogFilter;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.coords.WorldCoords;
import jsky.util.gui.SwingWorker;
import jsky.util.gui.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.spModel.target.env.OptionsList.UpdateOps.append;
import static jsky.app.ot.util.OtColor.*;

/**
 * This is the editor for the target list component.
 */
public final class EdCompTargetList extends OtItemEditor<ISPObsComponent, TargetObsComp>
        implements TelescopePosWatcher, ActionListener {

    private static final Logger LOG = Logger.getLogger(EdCompTargetList.class.getName());
    private static final String NON_SIDEREAL_TARGET = "Nonsidereal";
    private static final String EXTRA_URL_INFO = "/mimetype=full-rec";
    private static final String PROPER_MOTION_COL_ID = "pm1";
    private static final String SIMBAD_CATALOG_SHORTNAME = "simbad";
    private static final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    static {
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Table listing telescope positions
    private final TelescopePosTableWidget _tpTable;

    // The GUI layout panel
    private final TelescopeForm _w;
    private final SiderealEditor _siderealEditor;
    private final TrackingEditor _trackingEditor;
    private final JToggleButton _trackingButton;

    private final MagnitudeEditor _nonsideMagEditor;

    // Current position being edited
    private SPTarget _curPos;

    // Current group being edited
    private GuideGroup _curGroup;

    // Helper class for dealing with NonSidereal targets.
    private final NonSiderealTargetSupport _nonSiderealTargetSup;

    // If true, ignore change events for the current position
    private boolean _ignorePosUpdate = false;

    // Horizons Operations
    private final HashMap<HorizonsAction.Type, HorizonsAction> _horizonsOperations;

    // allows to control input text to be entered in the _w.calendarTime component
    // which contains the time (HH:MM:SS) for when an Horizons query should be made
    private TimeDocument _timeDocument;

    private final AgsContextPublisher _agsPub = new AgsContextPublisher();
    /**
     * The constructor initializes the user interface.
     */
    public EdCompTargetList() {

        _w = new TelescopeForm(this);

        //Callback for AGS visibiity
        _w.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent componentEvent) {
                toggleAgsGuiElements();
            }
        });


        _siderealEditor = new SiderealEditor();
        _trackingEditor = new TrackingEditor();

        // Tracking Editor use My Doggy style to match the p2 checker
        _trackingButton = new JToggleButton("Tracking Details") {{
            setUI(new RotatedButtonUI(RotatedButtonUI.Orientation.topToBottom));
            setBackground(VERY_LIGHT_GREY);
        }};

        _w.extrasFolder.add(_siderealEditor.getComponent(), "sidereal");

        // TODO: Being a bit lazy here.  Ideally we should strip out all the
        // TODO: nonsidereal widgets from the TelescopeForm and make them
        // TODO: into a proper editor like the SiderealEditor.
        _nonsideMagEditor = new MagnitudeEditor();
        JPanel nonsid = wrapNonsidereal(_w.nonsiderealPW, _nonsideMagEditor);
        _w.extrasFolder.add(nonsid, "nonsidereal");

        //make the planet stuff non-visible
        _w.planetsPanel.setVisible(false);
        _nonSiderealTargetSup = new NonSiderealTargetSupport(_w, _curPos);

        _initPosEditor();
        _initWidgets();
        _initNameServerChoices();
        _initTargetName();
        _initGuideGroupName();
        _initXYAxis();
        _initSystemChoices();
        _initConicDetails();
        _initPlanetsRadioButtons();

        // *** Position Table
        _tpTable = _w.positionTable;

        // I realize this isn't the right way to do these things...
        _tpTable.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent event) {
            }

            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    // Make the delete and backspace buttons delete selected
                    // positions.
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        _w.removeButton.doClick();
                        break;
                }
            }

            public void keyReleased(KeyEvent event) {
            }
        });

        //Horizons Operations
        _horizonsOperations = new HorizonsActionContainer().getActions();

        _agsPub.subscribe(new AgsContextSubscriber() {
            @Override public void notify(ISPObservation obs, AgsContext oldOptions, AgsContext newOptions) {
                updateGuiding();
            }
        });
    }

    private static JPanel wrapNonsidereal(JPanel nonsiderealPanel, MagnitudeEditor med) {
        JPanel pan = new JPanel(new GridBagLayout());

        pan.add(new JLabel("Brightness"), new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            anchor = WEST;
            fill = HORIZONTAL;
            insets = new Insets(0, 0, 5, 20);
        }});
        pan.add(med.getComponent(), new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            anchor = WEST;
            fill = BOTH;
            weighty = 1.0;
            insets = new Insets(0, 0, 0, 20);
        }});
        pan.add(nonsiderealPanel, new GridBagConstraints() {{
            gridx = 1;
            gridy = 0;
            gridheight = 2;
            fill = BOTH;
            weightx = 1.0;
            weighty = 1.0;
        }});

        return pan;
    }

    /**
     * Set the contained target's RA and Dec from Strings in HMS/DMS format and notify listeners.
     * Invalid values are replaced with 00:00:00.
     */
    private static void setHmsDms(SPTarget spTarget, final String hms, final String dms) {
        synchronized (spTarget) {
            try {
                spTarget.getTarget().getRa().setValue(hms);
            } catch (final IllegalArgumentException ex) {
                spTarget.getTarget().getRa().setValue("00:00:00.0");
            }
            try {
                spTarget.getTarget().getDec().setValue(dms);
            } catch( final IllegalArgumentException ex) {
                spTarget.getTarget().getDec().setValue("00:00:00.0");
            }
        }
        spTarget.notifyOfGenericUpdate();
    }

    // Initialize the widgets involved in editing positions. This includes the
    // RA/Dec + sidereal or nonsidereal information.  Takes the GUI built by
    // JFormDesigner in _w.coordinatesPanel and morphs it a bit.  This is all
    // pretty gross.  Trying to avoid interacting with JFormDesigner as much
    // as possible.
    private void _initPosEditor() {
        // Take out the JFormDesigner panel, to move it inside another panel.
        _w.remove(_w.coordinatesPanel);

        // Pan will contain and take the place of _w.coordinatesPanel.  It
        // contains a "content" panel and the "Tracking Details" sideways
        // button.
        JPanel contentPanel = new JPanel(new GridBagLayout());

        // Content will contain the JFormDesigner panel and, when present, the
        // contents of the Tracking Details widgets.
        final JPanel content = new JPanel(new BorderLayout(10, 0));
        contentPanel.add(content, new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            weightx = 1.0;
            weighty = 1.0;
            fill = BOTH;
            anchor = NORTHWEST;
        }});
        content.add(_w.coordinatesPanel, BorderLayout.CENTER);

        // Add a decorative border to the coordinates panel.
        _w.coordinatesPanel.setBorder(BorderFactory.createCompoundBorder(
                new ThinBorder(ThinBorder.RAISED),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));


        final JPanel editor = wrapTrackingEditor(_trackingEditor.getComponent());
        addActivateEditorAction(content, _trackingButton, editor);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.PAGE_AXIS));
        buttonsPanel.add(_trackingButton);

        contentPanel.add(buttonsPanel, new GridBagConstraints() {{
            gridx = 1;
            gridy = 0;
            anchor = NORTH;
            insets = new Insets(0, 1, 0, 0);
        }});

        // Finish replacing the JFormDesigner _w.coordinatesPanel with our
        // wrapper
        _w.add(contentPanel, new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            fill = HORIZONTAL;
            weightx = 1.0;
            insets = new Insets(5, 0, 5, 0);
        }});
    }

    // Wraps the tracking details editor with a border and a lighter background
    // so that it shows.
    private static JPanel wrapTrackingEditor(Component component) {
        JPanel wrapper = new JPanel(new BorderLayout()) {{
            setBackground(LIGHT_GREY);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARKER_BG_GREY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
        }};
        wrapper.add(component, BorderLayout.CENTER);
        return wrapper;
    }

    private void addActivateEditorAction(final JPanel content, final JToggleButton button, final JPanel panel) {
        // When the tracking details button is pressed, show the tracking
        // details panel to the side of the normal content _w.coordinatesPanel
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JToggleButton tb = (JToggleButton) e.getSource();
                if (tb.isSelected()) {
                    tb.setBackground(LIGHT_ORANGE);
                    content.add(panel, BorderLayout.EAST);
                    content.validate();
                } else {
                    tb.setBackground(VERY_LIGHT_GREY);
                    content.remove(panel);
                    content.validate();
                }
            }
        });

    }

    private void _initWidgets() {
        _initNonSiderealTimeWidgets();

        _initMenuButton(_w.newMenuBar, _w.newMenu, "eclipse/add_menu.gif");

        _w.removeButton.addActionListener(this);
        _w.removeButton.setText("");
        _w.removeButton.setIcon(Resources.getIcon("eclipse/remove.gif"));
        ButtonFlattener.flatten(_w.removeButton);

        _w.copyButton.setIcon(Resources.getIcon("eclipse/copy.gif"));
        _w.copyButton.setText("");
        ButtonFlattener.flatten(_w.copyButton);
        _w.copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copySelectedPosition(getNode(), getDataObject());
            }
        });

        _w.pasteButton.setIcon(Resources.getIcon("eclipse/paste.gif"));
        _w.pasteButton.setText("");
        ButtonFlattener.flatten(_w.pasteButton);
        _w.pasteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_curPos != null) {
                    pasteSelectedPosition(getNode(), getDataObject());
                    _curPos.notifyOfGenericUpdate();
                } else if (_curGroup != null) {
                    pasteSelectedPosition(getNode(), getDataObject());
                }
            }
        });

        _w.duplicateButton.setIcon(Resources.getIcon("eclipse/duplicate.gif"));
        _w.duplicateButton.setText("");
        ButtonFlattener.flatten(_w.duplicateButton);
        _w.duplicateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _duplicateSelected(getNode(), getDataObject());
            }
        });

        _w.primaryButton.setIcon(Resources.getIcon("eclipse/radiobuttons.gif"));
        _w.primaryButton.setText("");
        ButtonFlattener.flatten(_w.primaryButton);
        _w.primaryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _w.positionTable.updatePrimaryStar();

            }
        });

        _w.autoGuideStarGuiderSelector.addSelectionListener(new AgsSelectorControl.Listener() {
            @Override public void agsStrategyUpdated(Option<AgsStrategy> strategy) {
                AgsStrategyUtil.setSelection(getContextObservation(), strategy);
            }
        });
        _w.autoGuideStarButton.addActionListener(this);
        _w.manualGuideStarButton.addActionListener(this);

        _w.resolveButton.addActionListener(this);
        _w.resolveButton.setText("");
        _w.resolveButton.setIcon(Resources.getIcon("eclipse/search.gif"));
        ButtonFlattener.flatten(_w.resolveButton);
        _w.setBaseButton.addActionListener(this);

        _w.calendarTime.addActionListener(this);

        _w.timeRangePlotButton.addActionListener(this);
        _w.updateRaDecButton.addActionListener(this);

        _initMenuButton(_w.nameServerBar, _w.nameServer, "eclipse/menu-trimmed.gif");
    }

    @Override
    protected void updateEnabledState(boolean enabled) {
        super.updateEnabledState(enabled);

        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        _w.tag.setEnabled(enabled && env.getBase() != _curPos);

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.setEnabled(enabled && inst != null);
    }

    // Initialize a JMenuBar and JMenu to get a decent looking menu button.
    private void _initMenuButton(JMenuBar bar, final JMenu menu, String iconPath) {
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setOpaque(false);
        final Icon icon = Resources.getIcon(iconPath);
        menu.setIcon(Resources.getIcon(iconPath));
        menu.setText("");
        ButtonFlattener.flatten(menu);

        // Unfortunately the MenuUI thing ignores the rollover icon.  Watch
        // for mouse events and handle in manually.
        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                menu.setIcon(menu.getRolloverIcon());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                menu.setIcon(icon);
            }
        });
    }

    /**
     * Initialize the calendar and the combo box for specifing a time
     */
    private void _initNonSiderealTimeWidgets() {
        DefaultComboBoxModel model = new DefaultComboBoxModel<>(TimeConfig.values());

        //Add space for the editable field. We will use a String here
        //model.insertElementAt(timeFormatter.format(new Date()), 0);

        _w.calendarTime.setModel(model);
        _w.calendarTime.setRenderer(new BasicComboBoxRenderer() {
            public Component getListCellRendererComponent(JList jList, Object
                    object, int index, boolean isSelected, boolean hasFocus) {

                String text = object.toString();
                if (object instanceof Date) {
                    text = timeFormatter.format((Date) object);
                } else if (object instanceof TimeConfig) {
                    text = ((TimeConfig) object).displayValue();
                }
                return super.getListCellRendererComponent(jList, text, index, isSelected, hasFocus);
            }
        });

        ComboBoxEditor editor = _w.calendarTime.getEditor();

        JTextField tf = (JTextField) editor.getEditorComponent();
        _timeDocument = new TimeDocument(tf);

        _timeDocument.setTime(timeFormatter.format(new Date()));
        tf.setDocument(_timeDocument);

        //set the calendar widget to timezone UTC
        _w.calendarDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private void _initXYAxis() {
        _w.xaxis.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                _setCurPos();
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                _setCurPos();
            }
        });
        _w.yaxis.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                _setCurPos();
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                _setCurPos();
            }
        });
    }

    // Add listeners to the entry widgets in the conic target tab
    private void _initConicDetails() {
        _nonSiderealTargetSup.initListeners(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbw) {
                _setConicPos((NumberBoxWidget) tbw);
            }

            public void textBoxAction(TextBoxWidget tbw) {
                _setConicPos((NumberBoxWidget) tbw);
            }
        });
    }

    private void _initTargetName() {
        _w.targetName.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                String name = tbwe.getText();
                if (name != null) name = name.trim();
                _curPos.deleteWatcher(EdCompTargetList.this);
                _curPos.getTarget().setName(name);
                _curPos.notifyOfGenericUpdate();
                _curPos.addWatcher(EdCompTargetList.this);
                _w.resolveButton.setEnabled(!"".equals(name));
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                _resolveName(HorizonsAction.Type.GET_ORBITAL_ELEMENTS, null);
            }
        });
        _w.targetName.setMinimumSize(_w.targetName.getPreferredSize());
    }

    private void _initGuideGroupName() {
        _w.guideGroupName.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String name = _w.guideGroupName.getText();
                        // don't trim, otherwise user can't include space in group name
//                        if (name != null) name = name.trim();
                        GuideGroup newGroup = _curGroup.setName(name);
                        TargetEnvironment env = getDataObject().getTargetEnvironment();
                        GuideEnvironment ge = env.getGuideEnvironment();
                        ImList<GuideGroup> options = ge.getOptions();
                        List<GuideGroup> list = new ArrayList<>(options.size());
                        for (GuideGroup g : options) {
                            list.add(g == _curGroup ? newGroup : g);
                        }
                        _curGroup = newGroup;
                        getDataObject().setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                        _w.guideGroupName.requestFocus(); // otherwise focus is lost during event handling
                    }
                });
            }

            public void textBoxAction(TextBoxWidget tbwe) {
            }
        });
        _w.guideGroupName.setMinimumSize(_w.guideGroupName.getPreferredSize());
    }

    private interface PositionType {
        boolean isAvailable();
        void morphTarget(TargetObsComp obsComp, SPTarget target);
        boolean isMember(TargetEnvironment env, SPTarget target);
    }

    private enum BasePositionType implements PositionType {
        instance;

        public boolean isAvailable() {
            return true;
        }

        public void morphTarget(TargetObsComp obsComp, SPTarget target) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            if (isMember(env, target)) return;
            env = env.removeTarget(target);

            SPTarget base = env.getBase();

            GuideEnvironment genv = env.getGuideEnvironment();
            ImList<SPTarget> user = env.getUserTargets().append(base);

            env = TargetEnvironment.create(target, genv, user);
            obsComp.setTargetEnvironment(env);
        }

        public boolean isMember(TargetEnvironment env, SPTarget target) {
            return (env.getBase() == target);
        }

        public String toString() {
            return TargetEnvironment.BASE_NAME;
        }
    }

    private class GuidePositionType implements PositionType {
        private final GuideProbe guider;
        private final boolean available;

        GuidePositionType(GuideProbe guider, boolean available) {
            this.guider = guider;
            this.available = available;
        }

        public boolean isAvailable() {
            return available;
        }

        public void morphTarget(TargetObsComp obsComp, SPTarget target) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            if (isMember(env, target)) return;
            env = env.removeTarget(target);

            GuideProbeTargets gt;
            Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (gtOpt.isEmpty()) {
                gt = GuideProbeTargets.create(guider, ImCollections.singletonList(target));
            } else {
                gt = gtOpt.getValue();
                gt = gt.update(OptionsList.UpdateOps.appendAsPrimary(target));
            }
            env = env.putPrimaryGuideProbeTargets(gt);
            obsComp.setTargetEnvironment(env);
        }

        public boolean isMember(TargetEnvironment env, SPTarget target) {
            for (GuideGroup group : env.getGroups()) {
                Option<GuideProbeTargets> gtOpt = group.get(guider);
                if (!gtOpt.isEmpty() && gtOpt.getValue().getOptions().contains(target)) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return guider.getKey();
        }
    }

    private enum UserPositionType implements PositionType {
        instance;

        public boolean isAvailable() {
            return true;
        }

        public void morphTarget(TargetObsComp obsComp, SPTarget target) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            if (isMember(env, target)) return;

            env = env.removeTarget(target);
            env = env.setUserTargets(env.getUserTargets().append(target));
            obsComp.setTargetEnvironment(env);
        }

        public boolean isMember(TargetEnvironment env, SPTarget target) {
            return env.getUserTargets().contains(target);
        }

        public String toString() {
            return TargetEnvironment.USER_NAME;
        }
    }

    private final ActionListener _tagListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            PositionType pt = (PositionType) _w.tag.getSelectedItem();
            pt.morphTarget(getDataObject(), _curPos);
            _handleSelectionUpdate(_curPos);
        }
    };

    /**
     * A renderer of target type options.  Shows a guider type that isn't
     * available in the current context with a warning icon.
     */
    private static final DefaultListCellRenderer TAG_RENDERER = new DefaultListCellRenderer() {
        private final Icon errorIcon = Resources.getIcon("eclipse/error.gif");

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            PositionType pt = (PositionType) value;
            if (!pt.isAvailable()) {
                lab.setFont(lab.getFont().deriveFont(Font.ITALIC));
                lab.setIcon(errorIcon);
            }
            return lab;
        }
    };

    // Get a reference to the "Tag" drop down, and initialize its choices
    private void _initTagChoices() {

        // Get all the legally available guiders in the current context.
        Set<GuideProbe> avail = GuideProbeUtil.instance.getAvailableGuiders(getContextObservation());
        Set<GuideProbe> guiders = new HashSet<>(avail);
        TargetEnvironment env = getDataObject().getTargetEnvironment();

        // Get the set of guiders that are referenced but not legal in this
        // context, if any.  Any "available" guider is legal, anything left
        // over is referenced but not really available.
        Set<GuideProbe> illegalSet = env.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        illegalSet.removeAll(avail);

        // Determine whether the current position is one of these illegal
        // guiders.  If so, we add the guide probe to the list of choices
        // so that this target may be selected in order to change its type or
        // delete it.
        GuideProbe illegal = null;
        for (GuideProbe guider : illegalSet) {
            Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (gtOpt.getValue().getOptions().contains(_curPos)) {
                illegal = guider;
                guiders.add(guider);
            }
        }

        // Sort the list of guiders.
        List<GuideProbe> guidersList = new ArrayList<>(guiders);
        Collections.sort(guidersList, GuideProbe.KeyComparator.instance);

        // Make a list of PositionTypes that are legal in the current
        // observation context.
        PositionType[] ptA;
        ptA = new PositionType[2 + guiders.size()];

        int index = 0;
        ptA[index++] = BasePositionType.instance;
        for (GuideProbe guider : guidersList) {
            ptA[index++] = new GuidePositionType(guider, guider != illegal);
        }
        ptA[index] = UserPositionType.instance;

        _w.tag.removeActionListener(_tagListener);
        _w.tag.setModel(new DefaultComboBoxModel(ptA));
        _w.tag.setEnabled(isEnabled() && (env.getBase() != _curPos));
        _w.tag.addActionListener(_tagListener);

        _w.tag.setRenderer(TAG_RENDERER);
    }

    private Option<Catalog> _selectedNameServer = None.instance();

    // Initialize the choices for name servers for resolving the Name field to RA,Dec
    private void _initNameServerChoices() {
        SkycatConfigFile cf = SkycatConfigFile.getConfigFile();

        // Create radio button options for each of the name servers.
        final ButtonGroup grp = new ButtonGroup();
        List<Catalog> nameServers = cf.getNameServers();
        for (final Catalog ns : nameServers) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(ns.getName()) {{
                addActionListener(new ActionListener() {
                    // When selected, update the currently selected name server
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        _selectedNameServer = new Some<>(ns);
                    }
                });
            }};
            grp.add(mi);
            _w.nameServer.add(mi);
        }

        // Set the initial name server options
        if (nameServers.size() > 0) {
            _selectedNameServer = new Some<>(nameServers.get(0));
            grp.setSelected(((JMenuItem) _w.nameServer.getMenuComponent(0)).getModel(), true);
        }
    }

    private static final class TargetClipboard {
        private ITarget target;
        private GuideGroup group;
        private ImList<Magnitude> mag;

        static Option<TargetClipboard> copy(TargetEnvironment env, ISPObsComponent obsComponent) {
            if (obsComponent == null) return None.instance();

            final SPTarget target = TargetSelection.get(env, obsComponent);
            if (target == null) {
                GuideGroup group = TargetSelection.getGuideGroup(env, obsComponent);
                if (group == null) {
                    return None.instance();
                }
                return new Some<>(new TargetClipboard(group));
            }
            return new Some<>(new TargetClipboard(target));
        }

        TargetClipboard(SPTarget spTarget) {
            this.target = (ITarget) spTarget.getTarget().clone();
            this.mag = spTarget.getTarget().getMagnitudes();
        }

        TargetClipboard(GuideGroup group) {
            this.group = group;
        }

        // Groups in their entirety should be copied, pasted, and duplicated by the existing
        // copy, paste, and duplicate buttons.  Disallow pasting a group on top of an individual target.
        // Pasting on top of a group should replace the group contents just as the target paste replaces
        // the coordinates of the selected target.
        void paste(ISPObsComponent obsComponent, TargetObsComp dataObject) {
            if ((obsComponent == null) || (dataObject == null)) return;

            final SPTarget spTarget = TargetSelection.get(dataObject.getTargetEnvironment(), obsComponent);
            if (spTarget != null) {
                if (target != null) {
                    spTarget.setTarget((ITarget) target.clone());
                    spTarget.getTarget().setMagnitudes(mag);
                    spTarget.notifyOfGenericUpdate();
                }
            } else {
                final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
                if (group != null) {
                    if (this.group != null) {
                        GuideGroup newGroup = group.setAll(this.group.cloneTargets().getAll());
                        // XXX TODO: add a helper method in the model to replace a guide group
                        TargetEnvironment env = dataObject.getTargetEnvironment();
                        GuideEnvironment ge = dataObject.getTargetEnvironment().getGuideEnvironment();
                        ImList<GuideGroup> options = ge.getOptions();
                        ArrayList<GuideGroup> list = new ArrayList<>(options.size());
                        for (GuideGroup gg : options) {
                            list.add(gg == group ? newGroup : gg);
                        }
                        dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                    }
                }
            }
        }
    }

    // Duplicate the selected position or group
    private void _duplicateSelected(ISPObsComponent obsComponent, TargetObsComp dataObject) {
        if ((obsComponent == null) || (dataObject == null)) return;
        final SPTarget target = TargetSelection.get(dataObject.getTargetEnvironment(), obsComponent);
        if (target != null) {
            _duplicateSelectedPosition(dataObject, target);
        } else {
            final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
            if (group != null) {
                _duplicateSelectedGroup(dataObject, group);
            }
        }
    }

    // Duplicate the selected position
    private void _duplicateSelectedPosition(TargetObsComp dataObject, SPTarget target) {
        // Clone the target.
        ParamSet ps = target.getParamSet(new PioXmlFactory());
        SPTarget newTarget = new SPTarget();
        newTarget.setParamSet(ps);

        // Add it to the environment.  First we have to figure out
        // what it is.
        TargetEnvironment env = dataObject.getTargetEnvironment();

        // See if it is a guide star and duplicate it in the correct
        // GuideTargets list.
        boolean duplicated = false;
        env.getOrCreatePrimaryGuideGroup();
        List<GuideGroup> groups = new ArrayList<>();
        for (GuideGroup group : env.getGroups()) {
            for (GuideProbeTargets gt : group) {
                if (gt.getOptions().contains(target)) {
                    group = group.put(gt.update(append(newTarget)));
                    duplicated = true;
                    break;
                }
            }
            groups.add(group);
        }
        if (duplicated) {
            // Update groups list
            env = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
        } else {
            // Add as a user target
            env = env.setUserTargets(env.getUserTargets().append(newTarget));
        }

        dataObject.setTargetEnvironment(env);
    }

    // Duplicate the selected group
    private void _duplicateSelectedGroup(TargetObsComp dataObject, GuideGroup group) {
        TargetEnvironment env = dataObject.getTargetEnvironment();
        List<GuideGroup> groups = new ArrayList<>();
        groups.addAll(env.getGroups().toList());
        groups.add(group.cloneTargets());
        env = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
        dataObject.setTargetEnvironment(env);
        // save/restore tree state will leave last group closed, since there is one more, so expand it here
        _w.positionTable.expandGroup(groups.get(groups.size() - 1));
    }


    private static TargetClipboard clipboard;

    private static void copySelectedPosition(ISPObsComponent obsComponent, TargetObsComp dataObject) {
        Option<TargetClipboard> opt = TargetClipboard.copy(dataObject.getTargetEnvironment(), obsComponent);
        if (opt.isEmpty()) return;
        clipboard = opt.getValue();
    }

    private static void pasteSelectedPosition(ISPObsComponent obsComponent, TargetObsComp dataObject) {
        if (clipboard == null) return;
        clipboard.paste(obsComponent, dataObject);
    }

    // Try to resolve the name in the name field to RA,Dec coordinates and insert the result in
    // the RA,Dec text boxes.
    private void _resolveName(HorizonsAction.Type operationType, final ResolveNameListener listener) {
        String name = _w.targetName.getText().trim();
        //if non-sidereal, then do horizons query...
        if (_w.system.getSelectedItem() == NON_SIDEREAL_TARGET) {
            if (_curPos.getTarget() instanceof NamedTarget) {
                NamedTarget target = (NamedTarget) _curPos.getTarget();
                name = target.getSolarObject().getHorizonsId();
            }

            if (!name.isEmpty()) {
                _resolveNonSidereal(name, operationType, listener);
            }
        } else {
            if (!_selectedNameServer.isEmpty()) {
                Catalog nameServer = _selectedNameServer.getValue();
                if (name.length() != 0) {
                    QueryArgs queryArgs = new BasicQueryArgs(nameServer);
                    queryArgs.setId(name);
                    _setRaDecFromCatalog(nameServer, queryArgs);
                }
            }
        }
    }

    /**
     * Retrieves a date, that's built based on the information from
     * the calendar widget and the time widget
     */
    private Date _getDate() {
        //get the appropriate date to perform the query based on the calendar widget and
        //the time widget
        Date date = _w.calendarDate.getDate();
        //this date is at 00:00:00. Let's add the hours, minutes and sec from the time document

        calendar.setTimeZone(_w.calendarDate.getTimeZone());
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, _timeDocument.getHoursField());
        calendar.set(Calendar.MINUTE, _timeDocument.getMinutesField());
        calendar.set(Calendar.SECOND, _timeDocument.getSecondsField());

        return calendar.getTime();
    }


    /**
     * This is the main method to resolve non sidereal objects name to Orbital elements
     * and ephemeris. Will analyze the results and performa appropriate operations
     * based on them.
     *
     * @param name          The name of the object to be resolved
     * @param operationType The operation to be performed with the given <code>HorizonsReply</code>
     * @param listener      if not null, notified when the background thread completes successfully
     *                      (not called if there is an error)
     */
    private void _resolveNonSidereal(final String name, final HorizonsAction.Type operationType,
                                     final ResolveNameListener listener) {
        new AuthSwingWorker<HorizonsReply, Object>() {
            @Override
            protected HorizonsReply doInBackgroundWithSubject() {

                //for NamedTargets we don't resolve orbital elements
                if (_curPos.getTarget() instanceof NamedTarget &&
                        operationType == HorizonsAction.Type.GET_ORBITAL_ELEMENTS) {
                    return null;
                }

                //clear previous values
                //_clearNonSiderealInformation(operationType);

                //ignore events that trigger an Horizon's reply cache reset
                _nonSiderealTargetSup.ignoreResetCacheEvents(true);
                //If it's a PI, we need to figure out where to set the horizons site
                final HorizonsService service = HorizonsService.getInstance();
                if (service == null) return null;

                if (!OTOptions.isStaff(getProgram().getProgramID())) {
                    SPProgramID id = null;
                    try {
                        id = getProgram().getProgramID();
                    } catch (NullPointerException e) {
                        //this shouldn't happen
                    }
                    service.setSite(id);
                }


                //Get the Date to start the query
                Date date = _getDate();

                //lets see if we can use the existing ephemeris
                HorizonsReply lastReply = service.getLastResult();
                if (lastReply != null && name.equals(service.getObjectId())) {
                    //so, apparently nothing has changed
                    boolean workable = true;
                    if (operationType == HorizonsAction.Type.GET_ORBITAL_ELEMENTS) {
                        if (!lastReply.hasOrbitalElements()) {
                            //ups. This reply doesn't quite work.
                            workable = false;
                        }
                    }

                    //let's see if we have ephemeris, and with that we are
                    //okay (the ephemeris is usable for operations that doesn't
                    //involve updating orbital elements.
                    if (lastReply.hasEphemeris() && workable) {
                        EphemerisEntry firstEntry = lastReply.getEphemeris().get(0);
                        if (date.equals(firstEntry.getDate())) {
                            //Same results
                            return lastReply;
                        }
                    }

                }


                service.setInitialDate(date);

                service.setObjectId(name);

                service.setObjectType(null);

                ITarget.Tag tag = (ITarget.Tag) _w.orbitalElementFormat.getSelectedItem();
                switch (tag) {
                    case JPL_MINOR_BODY:   service.setObjectType(HorizonsQuery.ObjectType.COMET);      break;
                    case MPC_MINOR_PLANET: service.setObjectType(HorizonsQuery.ObjectType.MINOR_BODY); break;
                    case NAMED:            service.setObjectType(HorizonsQuery.ObjectType.MAJOR_BODY); break;
                }
                _nonSiderealTargetSup.ignoreResetCacheEvents(false);

                HorizonsReply reply = service.execute();

                if (reply == null || reply.getReplyType() == HorizonsReply.ReplyType.MAJOR_PLANET) {
                    if (service.getObjectType() == HorizonsQuery.ObjectType.COMET) {
                        service.setObjectType(HorizonsQuery.ObjectType.MINOR_BODY);
                        reply = service.execute();
                    } else if (service.getObjectType() == HorizonsQuery.ObjectType.MINOR_BODY) {
                        service.setObjectType(HorizonsQuery.ObjectType.COMET);
                        reply = service.execute();
                    }
                }

                return reply;
            }

            @Override
            public void done() {
                final HorizonsReply reply;
                try {
                    reply = get();
                } catch (InterruptedException ex) {
                    return;
                } catch (ExecutionException ex) {
                    final String message = "An error occurred fetching results for: " + name.toUpperCase();
                    LOG.log(Level.WARNING, message, ex);
                    DialogUtil.message(message);
                    return;
                }

                if ((reply == null) || (reply.getReplyType() == HorizonsReply.ReplyType.NO_RESULTS)) {
                    DialogUtil.message("No results were found for: " + name.toUpperCase());
                    return;
                }

                if (reply.getReplyType() == HorizonsReply.ReplyType.MAJOR_PLANET
                        && !(_curPos.getTarget() instanceof NamedTarget)) {
                    DialogUtil.message("Can't solve the given ID to any minor body");
                    return;
                }

                if (reply.getReplyType() == HorizonsReply.ReplyType.SPACECRAFT) {
                    DialogUtil.message("Horizons suggests this is a spacecraft. Sorry, but OT can't use spacecrafts");
                    return;
                }

                if (reply.getObjectType() == null) {//it will be null if you press "cancel" in the window with multiple responses
                    return;
                }

                if ((_curPos.getTarget() instanceof NamedTarget) && ((NamedTarget) _curPos.getTarget()).getSolarObject() == NamedTarget.SolarObject.PLUTO) {
                    reply.setReplyType(HorizonsReply.ReplyType.MAJOR_PLANET);
                }


                //ignore events that would reset the Horizons results cache
                _nonSiderealTargetSup.ignoreResetCacheEvents(true);
                //if a multiple answer was executed, then we need to recover the Id used
                final HorizonsService service = HorizonsService.getInstance();
                if (service != null) {
                    final String objectId = service.getObjectId();
                    _curPos.getTarget().setName(objectId);
                    _curPos.notifyOfGenericUpdate();
                }


                HorizonsAction action = _horizonsOperations.get(operationType);
                try {
                    action.execute(reply);
                } catch (NullPointerException ex) {
                    Logger LOG = Logger.getLogger(EdCompTargetList.class.getName());
                    LOG.log(Level.INFO, "Probable problem parsing the reply from JPL", ex);
                    return;
                }
                if (_curPos.getTarget() instanceof NonSiderealTarget) {
                    NonSiderealTarget oldTarget = (NonSiderealTarget) _curPos.getTarget();
                    switch (reply.getObjectType()) {
                        case COMET: {
                            final ConicTarget target = newOrExistingTarget(ITarget.Tag.JPL_MINOR_BODY);
                            _nonSiderealTargetSup.showNonSiderealTarget(target);
                            _w.orbitalElementFormat.setValue(ITarget.Tag.JPL_MINOR_BODY);
                            _curPos.setTarget(target);
                            }
                            break;
                        case MINOR_BODY: {
                            final ConicTarget target = newOrExistingTarget(ITarget.Tag.MPC_MINOR_PLANET);
                            _nonSiderealTargetSup.showNonSiderealTarget(target);
                            _w.orbitalElementFormat.setValue(ITarget.Tag.MPC_MINOR_PLANET);
                            _curPos.setTarget(target);
                            }
                            break;
                        case MAJOR_BODY: {
                            final NamedTarget target = (oldTarget instanceof NamedTarget) ? (NamedTarget) oldTarget : new NamedTarget();
                            _nonSiderealTargetSup.showNonSiderealTarget(target);
                            _w.orbitalElementFormat.setValue(ITarget.Tag.NAMED);
                            _curPos.setTarget(target);
                            }
                            break;
                    }
                    //_nonSiderealTargetSup.showNonSiderealTarget(target, _curPos.getCoordSys().getName());

                }
                _nonSiderealTargetSup.ignoreResetCacheEvents(false);
                if (listener != null) {
                    listener.nameResolved();
                }
            }
        }.execute();
    }

    /**
     * Returns the current target if its system type matches the specified one, otherwise constructs
     * and returns a new conic target of the specified type.
     */
    private ConicTarget newOrExistingTarget(ITarget.Tag tag) {
        ITarget old = _curPos.getTarget();
        if (old instanceof ConicTarget && old.getTag() == tag)
            return (ConicTarget) old;
        else
            return (ConicTarget) ITarget.forTag(tag);
    }

    // Query the given catalog using the given arguments and set the current target position
    // from the results. (XXX some of this should go in a helper class?)
    private void _setRaDecFromCatalog(final Catalog cat, final QueryArgs queryArgs) {
        if (cat != null) {
            new SwingWorker() {
                public Object construct() {
                    try {
                        String originalUrls[] = null;
                        if (cat instanceof SkycatCatalog) {
                            SkycatCatalog skycat = (SkycatCatalog) cat;
                            if (skycat.getShortName().contains(SIMBAD_CATALOG_SHORTNAME)) {
                                skycat.addCatalogFilter(FullMimeSimbadCatalogFilter.getFilter());
                                int n = skycat.getConfigEntry().getNumURLs();
                                String[] urls = new String[n];
                                originalUrls = new String[n];
                                for (int i = 0; i < n; i++) {
                                    String urlStr = skycat.getConfigEntry().getURL(i);
                                    originalUrls[i] = urlStr;
                                    urlStr += EXTRA_URL_INFO;
                                    urls[i] = urlStr;
                                }
                                skycat.getConfigEntry().setURLs(urls);
                            }
                        }
                        queryArgs.setMaxRows(1);
                        QueryResult r = cat.query(queryArgs);
                        //Return the URLs to the previous state
                        if (cat instanceof SkycatCatalog) {
                            SkycatCatalog skycat = (SkycatCatalog) cat;
                            if (skycat.getShortName().contains(SIMBAD_CATALOG_SHORTNAME)) {
                                skycat.getConfigEntry().setURLs(originalUrls);
                            }
                        }

                        if (r instanceof TableQueryResult) {
                            TableQueryResult tqr = (TableQueryResult) r;
                            if (tqr.getRowCount() > 0) {
                                return tqr;
                            }
                            throw new CatalogException("No objects were found.");
                        }
                        throw new CatalogException("Error contacting catalog server");
                    } catch (Exception e) {
                        return e;
                    }
                }

                public void finished() {
                    Object o = getValue();
                    if (o instanceof TableQueryResult) {
                        TableQueryResult tqr = (TableQueryResult) o;
                        int pm = tqr.getColumnIndex(PROPER_MOTION_COL_ID);
                        if (pm >= 0) {
                            Double pm1 = (Double) tqr.getValueAt(0, pm);
                            Double pm2 = (Double) tqr.getValueAt(0, pm + 1);
                            setPM(_curPos, pm1, pm2);
                        } else {
                            //SCT-301: If not found, then we reset the value to zero
                            setPM(_curPos, 0.0, 0.0);
                        }
                        if (tqr.getCoordinates(0) instanceof WorldCoords) {
                            WorldCoords pos = (WorldCoords) tqr.getCoordinates(0);
                            _w.xaxis.setText(pos.getRA().toString());
                            _w.yaxis.setText(pos.getDec().toString());
                            _setCurPos();
                        }
                    } else if (o instanceof Exception) {
                        DialogUtil.error(((Exception) o).getMessage());
                    }
                }

                private void setPM(SPTarget spt, double ra, double dec) {
                    final ITarget it = spt.getTarget();
                    if (it instanceof HmsDegTarget) {
                        final HmsDegTarget t = (HmsDegTarget) it;
                        t.setPropMotionRA(ra);
                        t.setPropMotionDec(dec);
                    }
                    spt.notifyOfGenericUpdate();
                }

            }.start();
        }
    }


    // Initialize the target System menu
    private void _initSystemChoices() {
        _w.system.clear();
        _w.system.addChoice(HmsDegTarget.TAG.tccName);
        _w.system.addChoice(NON_SIDEREAL_TARGET);
        _w.system.addWatcher(new DropDownListBoxWidgetWatcher() {
            public void dropDownListBoxAction(DropDownListBoxWidget dd, int i, String val) {
                _updateCoordSystem();
            }
        });
        _nonSiderealTargetSup.initOrbitalElementFormatChoices();
    }

    //Initialize the listeners for the radio buttons.
    private void _initPlanetsRadioButtons() {
        JRadioButton[] buttons = _w.planetButtons;
        for (JRadioButton button : buttons) {
            button.addActionListener(this);
        }
    }

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return _w;
    }

    /**
     * Initialize the editor with the given science program root
     * and node.
     */
    public void init() {
        setTargetObsComp();
        _w.manualGuideStarButton.setVisible(GuideStarSupport.supportsManualGuideStarSelection(getNode()));
        updateGuiding();
        _agsPub.watch(getContextObservation());
    }

    protected void cleanup() {
        _agsPub.watch(null);
        TargetSelection.deafTo(getContextTargetObsComp(), selectionListener);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);
        super.cleanup();
    }

    /**
     * Toggles the ags related gui elements depending on context.
     */
    private void toggleAgsGuiElements() {
        final boolean supports = GuideStarSupport.supportsAutoGuideStarSelection(getNode());
        // hide the ags related buttons
        _w.guidingControls.supportsAgs_$eq(supports);
    }

    // Guider panel property change listener to modify status and magnitude limits.
    private final PropertyChangeListener guidingPanelUpdater = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            updateGuiding((TargetEnvironment) evt.getNewValue());
        }
    };

    private void updateGuiding() {
        updateGuiding(getDataObject().getTargetEnvironment());
    }

    private Option<ObsContext> obsContext(final TargetEnvironment env) {
        return ObsContext.create(getContextObservation()).map(new Function1<ObsContext, ObsContext>() {
            @Override public ObsContext apply(ObsContext obsContext) {
                return obsContext.withTargets(env);
            }
        });
    }

    private void updateGuiding(final TargetEnvironment env) {
        toggleAgsGuiElements();
        final Option<ObsContext> ctx = obsContext(env);
        _w.guidingControls.update(ctx);
        _siderealEditor.updateGuiding(ctx, _curPos);
    }

    private final PropertyChangeListener selectionListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final ISPObsComponent node = getContextTargetObsComp();
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            final SPTarget target = TargetSelection.get(env, node);
            if (target != null) {
                _handleSelectionUpdate(target);
            } else {
                final GuideGroup grp = TargetSelection.getGuideGroup(env, node);
                if (grp != null) _handleSelectionUpdate(grp);
            }
        }
    };

    // Updates the enabled state of the primary guide target button when the
    // target environment changes.
    private final PropertyChangeListener primaryButtonUpdater = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            boolean enabled = false;
            if (_curPos != null) {
                TargetEnvironment env = getDataObject().getTargetEnvironment();
                ImList<GuideProbeTargets> gtList = env.getOrCreatePrimaryGuideGroup().getAllContaining(_curPos);
                enabled = gtList.size() > 0;
            } else if (_curGroup != null) {
                enabled = true;
            }
            _w.primaryButton.setEnabled(enabled && OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()));
        }
    };

    private void setTargetObsComp() {
        /* ===== */
        final ISPObsComponent node = getContextTargetObsComp();
        TargetSelection.listenTo(node, selectionListener);

        getDataObject().addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);

        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        _handleSelectionUpdate(TargetSelection.get(env, node));

        _initAddMenu(getDataObject());
        _tpTable.reinit(getDataObject());
    }

    // Initializes the menu of items that can be added to the target
    // environment.
    private void _initAddMenu(final TargetObsComp obsComp) {
        SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.removeAll();
        if (obsComp == null || inst == null) {
            _w.newMenu.setEnabled(false);
            return;
        }
        _w.newMenu.setEnabled(true);

        TargetEnvironment env = obsComp.getTargetEnvironment();

        if (inst.hasGuideProbes()) {
            List<GuideProbe> guiders = new ArrayList<>(env.getGuideEnvironment().getActiveGuiders());
            Collections.sort(guiders, GuideProbe.KeyComparator.instance);

            for (final GuideProbe probe : guiders) {
                _w.newMenu.add(new JMenuItem(probe.getKey()) {{
                    addActionListener(new AddGuideStarAction(obsComp, probe, _w.positionTable));
                }});
            }
        }

        _w.newMenu.add(new JMenuItem("User") {{
            addActionListener(new AddUserTargetAction(obsComp));
        }});

        if (inst.hasGuideProbes()) {
            _w.newMenu.addSeparator();
            final JMenuItem guideGroupMenu = _w.newMenu.add(new JMenuItem("Guide Group") {{
                addActionListener(new AddGroupAction(obsComp, _w.positionTable));
            }});

            // OT-34: disable create group menu if no guide stars defined
            _w.newMenu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    guideGroupMenu.setEnabled(obsComp.getTargetEnvironment().getGuideEnvironment().getTargets().size() != 0);
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                }
            });
        }
    }

    // Action that handles adding a new guide star when a probe is picked from
    // the add menu.
    private class AddGuideStarAction implements ActionListener {
        private final TargetObsComp obsComp;
        private final GuideProbe probe;
        private final TelescopePosTableWidget positionTable;

        AddGuideStarAction(TargetObsComp obsComp, GuideProbe probe, TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.probe = probe;
            this.positionTable = positionTable;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            GuideEnvironment ge = env.getGuideEnvironment();
            if (ge.getPrimary().isEmpty()) {
                ge = ge.setPrimary(env.getOrCreatePrimaryGuideGroup());
                env = env.setGuideEnvironment(ge);
            }

            // OT-16: add new guide star to selected group, if any, otherwise the primary group
            GuideGroup guideGroup = positionTable.getSelectedGroupOrParentGroup(env);
            Option<GuideProbeTargets> opt = guideGroup == null ?
                    env.getPrimaryGuideProbeTargets(probe) :
                    guideGroup.get(probe);
            if (guideGroup == null) {
                guideGroup = ge.getPrimary().getValue();
            }

            GuideProbeTargets targets;
            SPTarget target = new SPTarget();
            if (opt.isEmpty()) {
                targets = GuideProbeTargets.create(probe, target);
            } else {
                targets = opt.getValue().update(OptionsList.UpdateOps.appendAsPrimary(target));
            }
            obsComp.setTargetEnvironment(env.setGuideEnvironment(
                    env.getGuideEnvironment().putGuideProbeTargets(guideGroup, targets)));

            // XXX OT-35 hack to work around recursive call to TargetObsComp.setTargetEnvironment() in
            // SPProgData.ObsContextManager.update()
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    _showTargetTag();
                }
            });
        }
    }

    private static class AddUserTargetAction implements ActionListener {
        private final TargetObsComp obsComp;

        AddUserTargetAction(TargetObsComp obsComp) {
            this.obsComp = obsComp;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            env = env.setUserTargets(env.getUserTargets().append(new SPTarget()));
            obsComp.setTargetEnvironment(env);
        }
    }

    private static class AddGroupAction implements ActionListener {
        private final TargetObsComp obsComp;
        private final TelescopePosTableWidget positionTable;

        AddGroupAction(TargetObsComp obsComp, TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.positionTable = positionTable;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            GuideEnvironment ge = env.getGuideEnvironment();
            if (ge.getPrimary().isEmpty()) {
                ge = ge.setPrimary(env.getOrCreatePrimaryGuideGroup());
            }
            GuideGroup primaryGroup = ge.getPrimary().getValue();
            ImList<GuideGroup> options = ge.getOptions();
            GuideGroup group = GuideGroup.create(null);
            ImList<GuideGroup> groups = options.append(group);
            // OT-34: make new group primary and select it
            if (!positionTable.confirmGroupChange(primaryGroup, group)) return;
            obsComp.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(groups).selectPrimary(group)));
            if (groups.size() == 2) {
                // expand primary group tree node, which was previously hidden (implicit)
                positionTable.expandGroup(options.get(0));
            }
            // expand new group tree node
            positionTable.expandGroup(group);
        }
    }

    private static boolean enablePrimary(SPTarget target, TargetEnvironment env) {
        if (env.getBase() == target) return false;
        return !env.getUserTargets().contains(target);
    }

    private void _handleSelectionUpdate(SPTarget selTarget) {
        if (getDataObject() == null) return;
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        if (!env.getTargets().contains(selTarget)) return;

        final Option<ObsContext> ctx = obsContext(env);

        if (_curPos != null) _curPos.deleteWatcher(this);

        _curPos = selTarget;

        // Sidereal
        _siderealEditor.edit(ctx, selTarget);
        _trackingEditor.edit(ctx, selTarget);

        // Nonsidereal
        _nonSiderealTargetSup.updatePos(_curPos);
        _nonsideMagEditor.edit(ctx, selTarget);

        if (_curPos != null) {
            _curPos.addWatcher(this);
            _showPos();

            // can't remove base position, so disable button
            boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
            _w.removeButton.setEnabled(_curPos != env.getBase() && editable);
            _w.primaryButton.setEnabled(enablePrimary(selTarget, env) && editable);
        }
    }

    private void _handleSelectionUpdate(GuideGroup selGroup) {
        if (getDataObject() == null) return;
        TargetEnvironment env = getDataObject().getTargetEnvironment();
        if (!env.getGroups().contains(selGroup)) return;

        if (_curPos != null) _curPos.deleteWatcher(this);

        _curPos = null;
        _curGroup = selGroup;

        _showGroup();

        boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        _w.removeButton.setEnabled(editable);
        _w.primaryButton.setEnabled(editable);
    }

    /**
     * Show the current SPTarget.
     */
    private void _showPos() {
        _w.extrasFolder.setVisible(true);
        _w.objectGBW.setVisible(true);
        boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        _w.objectGBW.setEnabled(editable);
        _w.guideGroupPanel.setVisible(false);
        _trackingButton.setVisible(!_isNonSidereal());

        _initTagChoices();
        _showTargetTag();
        _updateTargetInformationUI();

        String name = _curPos.getTarget().getName();
        if (name != null) name = name.trim();
        _w.targetName.setValue(name);
        _w.resolveButton.setEnabled(editable && !"".equals(name));

        _w.xaxis.setValue(_curPos.getTarget().getRa().toString());
        _w.yaxis.setValue(_curPos.getTarget().getDec().toString());

        _setCoordSys();

        // update the display in the tabs
        if (_curPos.getTarget() instanceof NonSiderealTarget) {
            NonSiderealTarget nst = (NonSiderealTarget) _curPos.getTarget();
            _nonSiderealTargetSup.showNonSiderealTarget(nst);
        }
    }

    /**
     * Show/edit the current guide group name.
     */
    private void _showGroup() {
        _w.objectGBW.setVisible(false);
        _w.extrasFolder.setVisible(false);
        _w.guideGroupPanel.setVisible(true);
        if (_trackingButton.isSelected())
            _trackingButton.doClick();
        _trackingButton.setVisible(false);

        String name = _curGroup.getName().getOrElse("");
        // don't trim, otherwise user can't include space in group name
//        if (name != null) name = name.trim();
        _w.guideGroupName.setValue(name);
    }


    private void _showTargetTag() {
        TargetEnvironment env = getDataObject().getTargetEnvironment();
        for (int i = 0; i < _w.tag.getItemCount(); ++i) {
            PositionType pt = (PositionType) _w.tag.getItemAt(i);
            if (pt.isMember(env, _curPos)) {
                _w.tag.removeActionListener(_tagListener);
                _w.tag.setSelectedIndex(i);
                _w.tag.addActionListener(_tagListener);
                return;
            }
        }
    }

    /**
     * Called when the user types in one of the text boxes for the x,y coordinates
     */
    private void _setCurPos() {
        String ra = _w.xaxis.getText().trim(), dec = _w.yaxis.getText().trim();

        // allow "," instead of "."
        ra  = ra.replace(",", ".");
        dec = dec.replace(",", ".");

        if (!(dec.equals("-") || dec.equals("+"))) {
            _ignorePosUpdate = true;
            try {
                setHmsDms(_curPos, ra, dec);
            } finally {
                _ignorePosUpdate = false;
            }
            updateGuiding();
        }
    }


    // Called when the user types a value in one of the entries in the conic
    // target tab
    private void _setConicPos(NumberBoxWidget nbw) {
        ConicTarget target = (ConicTarget) _curPos.getTarget();
        _ignorePosUpdate = true;
        try {
            _nonSiderealTargetSup.setConicPos(target, nbw);
        } finally {
            _ignorePosUpdate = false;
        }
        _setCurPos(); // XXX not needed, except that this will cause an event to be fired
        updateGuiding();
    }


    // Show the coordinate system and update the other widgets based upon it.
    private void _setCoordSys() {
        if (_isNonSidereal()) {
            _w.system.setValue(NON_SIDEREAL_TARGET);
            _nonSiderealTargetSup.showOrbitalElementFormat();
        } else {
            _w.system.setValue(_curPos.getTarget().getTag().tccName);
        }
    }

    // Return true if the current position is a ConicTarget or a Planet Target
    private boolean _isNonSidereal() {
        return (_curPos.getTarget() instanceof NonSiderealTarget);
    }

    public void telescopePosUpdate(WatchablePos tp) {
        if (_ignorePosUpdate)
            return;

        if (tp != _curPos) {
            // This shouldn't happen ...
            System.out.println(getClass().getName() + ": received a position " +
                    " update for a position other than the current one: " + tp);
            return;
        }
        _showPos();
        updateGuiding();
    }


    // Update the current target to use the selected coordinate system
    private void _updateCoordSystem() {
        final ITarget.Tag tag;
        if (_w.system.getStringValue().equals(NON_SIDEREAL_TARGET)) {
            tag = ((ITarget.Tag) _w.orbitalElementFormat.getSelectedItem());
        } else {
            tag = ITarget.Tag.SIDEREAL;
        }
        _ignorePosUpdate = true;
        try {
            _curPos.setTargetType(tag);
        } finally {
            _ignorePosUpdate = false;
        }

        _showPos();
        updateGuiding();
    }

    // Update the enabled states of the target tabs based on the selected coordinate system
    private void _updateTargetInformationUI() {
        boolean isNonSidereal = _isNonSidereal();
        CardLayout cl = (CardLayout) _w.extrasFolder.getLayout();
        String tag = isNonSidereal ? "nonsidereal" : "sidereal";
        cl.show(_w.extrasFolder, tag);

        if (isNonSidereal) {
            // Tracking applies to sidereal targets.  If shown, hide.
            if (_trackingButton.isSelected()) _trackingButton.doClick();
        }
        _trackingButton.setVisible(!isNonSidereal);

        //disable the nameServer if we are a non-sidereal target
        _w.nameServer.setEnabled(!isNonSidereal);

        //disable the resolve name for Named Targets
        _w.resolveButton.setEnabled(!(_curPos.getTarget() instanceof NamedTarget));
    }

    // Used to notify listener when the resolveName() thread is done
    private interface ResolveNameListener {
        void nameResolved();
    }

    /**
     * Method to handle button actions.
     */
    public void actionPerformed(ActionEvent evt) {
        Object w = evt.getSource();
        TargetEnvironment env = getDataObject().getTargetEnvironment();

        if (w == _w.removeButton) {
            if (env.isBasePosition(_curPos)) {
                DialogUtil.error("You can't remove the Base Position.");
                return;
            }
            if (_curPos != null) {
                env = env.removeTarget(_curPos);
            } else if (_curGroup != null) {
                GuideGroup primary = env.getOrCreatePrimaryGuideGroup();
                if (_curGroup == primary) {
                    DialogUtil.error("You can't remove the primary guide group.");
                    return;
                }
                env = env.removeGroup(_curGroup);
                _curGroup = primary;
            }
            getDataObject().setTargetEnvironment(env);
            _handleSelectionUpdate(TargetSelection.get(env, getNode()));

        } else if (w == _w.manualGuideStarButton || w == _w.autoGuideStarButton) {
            try {
                boolean manual = w == _w.manualGuideStarButton;
                if (manual || GuideStarSupport.hasGemsComponent(getNode())) {
                    TelescopePosEditor tpe = TpeManager.open();
                    tpe.reset(getNode());
                    tpe.getImageWidget().guideStarSearch(manual);
                } else {
                    // In general, we don't want to pop open the TPE just to
                    // pick a guide star.
                    AgsClient.launch(getNode(), _w);
                }
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        } else if (w == _w.setBaseButton) {
            TelescopePosEditor tpe = TpeManager.get();
            if (tpe == null) {
                DialogUtil.message("The Position Editor must be opened for this feature to work.");
                return;
            }
            tpe.reset(getNode());
            WorldCoords basePos = tpe.getImageCenterLocation();
            if (basePos == null) {
                DialogUtil.message("Couldn't determine the image center.");
                return;
            }

            SPTarget base = env.getBase();
            base.getTarget().getRa().setAs(basePos.getRaDeg(), CoordinateParam.Units.DEGREES);
            base.getTarget().getDec().setAs(basePos.getDecDeg(), CoordinateParam.Units.DEGREES);
            base.notifyOfGenericUpdate();
        } else if (w == _w.resolveButton) {
            // REL-1063 Fix OT nonsidereal Solar System Object Horizons name resolution
            if (_curPos.getTarget() instanceof NamedTarget) {
                // For named objects like Moon, Saturn, etc don't get the orbital elements, just the position
                _resolveName(HorizonsAction.Type.UPDATE_POSITION, null);
            } else {
                _resolveName(HorizonsAction.Type.GET_ORBITAL_ELEMENTS, null);
            }
        } else if (w == _w.timeRangePlotButton) {
            _resolveName(HorizonsAction.Type.PLOT_EPHEMERIS, null);
        } else if (w == _w.updateRaDecButton) {
            // REL-1063 Fix OT nonsidereal Solar System Object Horizons name resolution
            if (_curPos.getTarget() instanceof NamedTarget) {
                // For named objects like Moon, Saturn, etc don't get the orbital elements, just the position
                _resolveName(HorizonsAction.Type.UPDATE_POSITION, null);
            } else {
                // REL-343: Force nonsidereal target name resolution on coordinate updates
                _resolveName(HorizonsAction.Type.GET_ORBITAL_ELEMENTS, new ResolveNameListener() {
                    @Override
                    public void nameResolved() {
                        _resolveName(HorizonsAction.Type.UPDATE_POSITION, null);
                    }
                });
            }
        } else if (w == _w.calendarTime) {
            Object o = _w.calendarTime.getSelectedItem();
            if (o instanceof TimeConfig) {
                TimeConfig tr = (TimeConfig) o;
                Date d = tr.getDate();
                String time = timeFormatter.format(d);
                _timeDocument.setTime(time);
                //we have to set the correct day in the calendar when
                // shortcuts are used.
                //because _w.calendarDate.setDate(d) doesn't work,
                // and Shane agreed :) we destroy the calendar a create a  new one with the correct date
                // (Shane's words => def uglyWorkaroundAcceptable(appName: String) = appName == "OT")
                //todo: figure out why  _w.calendarDate.setDate(d) doesn't work

                _w.panel1.remove(_w.calendarDate);
                _w.calendarDate = new JCalendarPopup(d, TimeZone.getTimeZone("UTC"));
                _w.panel1.add(_w.calendarDate, new CellConstraints().xy(5, 1));
                _w.calendarDate.revalidate();
                _w.calendarDate.repaint();

                //_w.calendarDate.set
            } else if (o instanceof String) {
                //just update the time document
                _timeDocument.setTime((String) o);
            }

        } else if (w instanceof JRadioButton) {
            String cmd = evt.getActionCommand().toUpperCase();
            if (_curPos.getTarget() instanceof NamedTarget) {
                NamedTarget target = (NamedTarget) _curPos.getTarget();
                try {
                    target.setSolarObject(NamedTarget.SolarObject.valueOf(cmd));
                    _setCurPos(); //not needed, but used to fire an event
                } catch (IllegalArgumentException ex) {
                    DialogUtil.error("Couldn't find a Planet called " + cmd);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Utility Classes  //////////////////////////
    ////////////////////////////////////////////////////////////////////////


    /**
     * The time configurations are pre-sets of dates the Horizons query should use to
     * gets its information.
     */
    private enum TimeConfig {

        NOW("Now") {
            public Date getDate() {
                return new Date();
            }
        },
        ONE_HOUR("1 Hour") {
            public Date getDate() {
                Date d = new Date();
                return new Date(d.getTime() + HOUR);
            }
        },
        TWO_HOUR("2 Hours") {
            public Date getDate() {
                Date d = new Date();
                return new Date(d.getTime() + HOUR * 2);
            }
        },

        THREE_HOUR("3 Hours") {
            public Date getDate() {
                Date d = new Date();
                return new Date(d.getTime() + HOUR * 3);
            }
        },

        FIVE_HOUR("5 Hours") {
            public Date getDate() {
                Date d = new Date();
                return new Date(d.getTime() + HOUR * 5);
            }
        },;

        private static final int HOUR = 1000 * 60 * 60;
        private final String _displayValue;

        private TimeConfig(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        /**
         * Return the <code>Date</code>  for the
         * given configuration
         *
         * @return the <code>Date</code> associated to this configuration
         */
        public abstract Date getDate();
    }


    /**
     * An Horizon Action encapsulates the operations to be performed
     * based on the results of an Horizons Query.
     */
    private interface HorizonsAction {
        public static enum Type {
            GET_ORBITAL_ELEMENTS,
            UPDATE_POSITION,
            PLOT_EPHEMERIS
        }

        /**
         * Executes the given operation using the given <code>HorizonsReply</code>
         */
        public void execute(HorizonsReply reply);
    }

    /**
     * A container for all the Horizons Actions that will be performed based on
     * the results of an query to the Horizons System.
     * </p>
     * Would have been great that actions would have been implemented as static
     * members, so I could have done this as a set of enums with the appropriate operation.
     * However, the actions operate on non-static member. I think it's possible
     * to create the appropriate methods to update the relevant objects when the underlying
     * EdCompTargetList changes, and use static methods anyway... but there is so many other
     * things to resolve yet that this is what we get.
     */
    private class HorizonsActionContainer {

        public HashMap<HorizonsAction.Type, HorizonsAction> getActions() {
            HashMap<HorizonsAction.Type, HorizonsAction> actions = new HashMap<>();
            actions.put(HorizonsAction.Type.GET_ORBITAL_ELEMENTS,
                    new UpdateOrbitalElements());
            actions.put(HorizonsAction.Type.UPDATE_POSITION,
                    new UpdatePosition());
            actions.put(HorizonsAction.Type.PLOT_EPHEMERIS,
                    new PlotEphemeris());
            return actions;
        }

        /**
         * Update the Orbital Elements of the NonSidereal object. Will update the
         * Ra,Dec (and time) with the latest information gotten. Also, it will
         * update the System Type of the conic target based on the type
         * of the answer.
         */
        private class UpdateOrbitalElements implements HorizonsAction {

            public void execute(HorizonsReply reply) {

                // Alright, we're going to replace the target if we get back a comet or minor
                // object, otherwise we do nothing. That's the old behavior.
                final ConicTarget target;
                String name = _w.targetName.getText().trim();

                // First construct the object and set the AQ
                switch (reply.getReplyType()) {
                    case COMET:
                        target = new ConicTarget(ITarget.Tag.JPL_MINOR_BODY);
                        if (reply.hasOrbitalElements()) {
                            OrbitalElements elements = reply.getOrbitalElements();
                            target.getAQ().setValue(elements.getValue(OrbitalElements.Name.QR));
                        }
                        _w.orbitalElementFormat.setSelectedItem(ITarget.Tag.JPL_MINOR_BODY);
                        _w.targetName.setText(name); //name is cleared if we move the element format
                        break;

                    case MINOR_OBJECT:
                        target = new ConicTarget(ITarget.Tag.MPC_MINOR_PLANET);
                        if (reply.hasOrbitalElements()) {
                            OrbitalElements elements = reply.getOrbitalElements();
                            target.getAQ().setValue(elements.getValue(OrbitalElements.Name.A));
                        }
                        _w.orbitalElementFormat.setSelectedItem(ITarget.Tag.MPC_MINOR_PLANET);
                        _w.targetName.setText(name); //name is cleared if we move the element format
                        break;

                    default:
                        return; /// ***
                }

                // Store horizons info, if available
                if (reply.hasObjectIdAndType()) {
                    target.setHorizonsObjectId(reply.getObjectId());
                    target.setHorizonsObjectTypeOrdinal(reply.getObjectType().ordinal());
                }

                // Set the orbital elements
                if (reply.hasOrbitalElements()) {
                    OrbitalElements elements = reply.getOrbitalElements();
                    target.getEpoch().setValue(elements.getValue(OrbitalElements.Name.EPOCH));
                    target.getEpochOfPeri().setValue(elements.getValue(OrbitalElements.Name.TP));
                    target.getANode().setValue(elements.getValue(OrbitalElements.Name.OM));
                    target.getPerihelion().setValue(elements.getValue(OrbitalElements.Name.W));
                    target.setE(elements.getValue(OrbitalElements.Name.EC));
                    target.getInclination().setValue(elements.getValue(OrbitalElements.Name.IN));
                    target.getLM().setValue(elements.getValue(OrbitalElements.Name.MA));
                }

                // Ok replace the target here since the ephemeris stuff updates the UI :-\
                _curPos.setTarget(target);

                //now, update current RA, Dec
                if (reply.hasEphemeris()) {
                    List<EphemerisEntry> ephemeris = reply.getEphemeris();
                    _processEphemeris(ephemeris);
                }

                // And make sure there's an update to the UI. This is so terrible.
                _curPos.notifyOfGenericUpdate();

            }
        }

        /**
         * Action to update the position of the object
         */
        private class UpdatePosition implements HorizonsAction {
            public void execute(HorizonsReply reply) {
                if (reply.hasEphemeris()) {
                    _processEphemeris(reply.getEphemeris());
                } else {
                    DialogUtil.error("No ephemeris available for object.");
                }

            }
        }


        /**
         * Action to plot the ephemeris for the given object
         */
        private class PlotEphemeris implements HorizonsAction {
            public void execute(HorizonsReply reply) {
                if (reply.hasEphemeris()) {
                    _drawEphemeris(reply.getEphemeris());
                } else {
                    DialogUtil.error("No ephemeris available for object.");
                }
            }
        }

        /**
         * Draw the given ephemeris in the TPE
         */
        private void _drawEphemeris(List<EphemerisEntry> ephemeris) {
            //first, we need to setup the base position
            _processEphemeris(ephemeris);
            if (ephemeris != null) {
                HorizonsPlotter.plot(getNode(), ephemeris);
            }
        }

        /**
         * Analyzes the ephemeris, and set the current position (and date) of the
         * Object based on that information. If the TPE is available,
         * will update its base position
         */
        private void _processEphemeris(List<EphemerisEntry> ephemeris) {
            if (ephemeris != null) {
                if (ephemeris.size() > 0) {
                    EphemerisEntry entry = ephemeris.get(0);
                    WorldCoords coords = entry.getCoordinates();
                    if (_curPos.getTarget() instanceof NonSiderealTarget) {
                        NonSiderealTarget target = (NonSiderealTarget) _curPos.getTarget();
                        _ignorePosUpdate = true;
                        try {
                            setHmsDms(_curPos, coords.getRA().toString(),
                                    coords.getDec().toString());
                        } finally {
                            _ignorePosUpdate = false;
                        }
                        target.setDateForPosition(entry.getDate());
                        updateGuiding();
                    }

                    // TPE REFACTOR -- what is this?
//                    TelescopePosEditor tpe = TpeManager.get();
//                    if (tpe != null) {
//                        SPTarget base = _targetObsComp.getTargetEnvironment().getBase();
//                        tpe.setBasePosition(base);
//                    }
                }
            }
        }
    }
}

