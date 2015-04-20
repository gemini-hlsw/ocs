// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
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
import jsky.app.ot.gemini.editor.targetComponent.details.TargetDetailPanel;
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
import javax.swing.border.BevelBorder;
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
 * This is the editor for the target list component. It is terrible.
 */
public final class EdCompTargetList extends OtItemEditor<ISPObsComponent, TargetObsComp> {

    // Static constants
//    private static final Logger   LOG                 = Logger.getLogger(EdCompTargetList.class.getName());
    private static final TimeZone UTC                 = TimeZone.getTimeZone("UTC");
    private static final String   NON_SIDEREAL_TARGET = "Nonsidereal";

    // Global variables \o/
    private static TargetClipboard clipboard;

    // Instance constants
    private final AgsContextPublisher _agsPub           = new AgsContextPublisher();
//    private final SiderealEditor      _siderealEditor   = new SiderealEditor();
//    private final TrackingEditor      _trackingEditor   = new TrackingEditor();
//    private final MagnitudeEditor     _nonsideMagEditor = new MagnitudeEditor();
//    private final JToggleButton       _trackingButton   = new TrackingButton();
    private final TargetDetailPanel   _detailEditor     = new TargetDetailPanel();

    // More constants, but they need access to `this` so we assign in the ctor
    private final TelescopeForm            _w;
    private final NonSiderealTargetSupport _nonSiderealTargetSup;
    private final TimeDocument             _timeDocument;
//
    // Stuff that varies with time
    private SPTarget        _curPos;
    private GuideGroup      _curGroup;
    private boolean         _ignorePosUpdate    = false;
//    private Option<Catalog> _selectedNameServer = None.instance();


    public EdCompTargetList() {

        // Finish initializing our constants
        _w = new TelescopeForm(this);
        _nonSiderealTargetSup = new NonSiderealTargetSupport(_w, _curPos);
        _timeDocument         = new TimeDocument((JTextField) _w.calendarTime.getEditor().getEditorComponent());

        // Move the tag menu up onto the menu bar, with a label
        _w.tag.getParent().remove(_w.tag);
        _w.buttonPanel.add(new JPanel() {{
            setOpaque(false);
            setLayout(new FlowLayout() {{
                setVgap(0);
            }});
            add(new JLabel("Type Tag: ") {{
                setOpaque(false);
                setVerticalAlignment(SwingConstants.CENTER);
            }});
            add(_w.tag);
        }}, 6); // index is the offset from the left

        //Callback for AGS visibiity
        _w.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent componentEvent) {
                toggleAgsGuiElements();
            }
        });

        // Add the sidereal editor, wrapped to make it less terrible
//        _w.extrasFolder.add(_siderealEditor.getComponent(), "sidereal");
        _w.extrasFolder.add(new JPanel(new GridBagLayout()) {{
            add(new JLabel("Brightness"), new GridBagConstraints() {{
                gridx = 0;
                gridy = 0;
                anchor = WEST;
                fill = HORIZONTAL;
                insets = new Insets(0, 0, 5, 20);
            }});
//            add(_nonsideMagEditor.getComponent(), new GridBagConstraints() {{
//                gridx = 0;
//                gridy = 1;
//                anchor = WEST;
//                fill = BOTH;
//                weighty = 1.0;
//                insets = new Insets(0, 0, 0, 20);
//            }});
            add(_w.nonsiderealPW, new GridBagConstraints() {{
                gridx = 1;
                gridy = 0;
                gridheight = 2;
                fill = BOTH;
                weightx = 1.0;
                weighty = 1.0;
            }});
        }}, "nonsidereal");

        _w.extrasFolder.setBorder(BorderFactory.createLineBorder(Color.GREEN));

        _w.planetsPanel.setVisible(false);

        // Re-wrap the coordinates panel
        _w.remove(_w.coordinatesPanel);
        final JPanel contentPanel = new JPanel(new GridBagLayout()) {{
            final JPanel content = new JPanel(new BorderLayout(10, 0)) {{
                add(_w.coordinatesPanel, BorderLayout.CENTER);
            }};
            add(content, new GridBagConstraints() {{
                gridx = 0;
                gridy = 0;
                weightx = 1.0;
                weighty = 1.0;
                fill = BOTH;
                anchor = NORTHWEST;
            }});
//            final JPanel buttonsPanel = new JPanel() {{
//                setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
//                add(_trackingButton);
//            }};
//            add(buttonsPanel, new GridBagConstraints() {{
//                gridx = 1;
//                gridy = 0;
//                anchor = NORTH;
//                insets = new Insets(0, 1, 0, 0);
//            }});
//            final JPanel panel = new JPanel(new BorderLayout()) {{
//                setBackground(LIGHT_GREY);
//                setBorder(BorderFactory.createCompoundBorder(
//                        BorderFactory.createLineBorder(DARKER_BG_GREY),
//                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
//                ));
//                add(_trackingEditor.getComponent(), BorderLayout.CENTER);
//            }};
//          // When the tracking details button is pressed, show the tracking
//          // details panel to the side of the normal content _w.coordinatesPanel
//            _trackingButton.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    final JToggleButton tb = (JToggleButton) e.getSource();
//                    if (tb.isSelected()) {
//                        tb.setBackground(LIGHT_ORANGE);
//                        content.add(panel, BorderLayout.EAST);
//                        content.validate();
//                    } else {
//                        tb.setBackground(VERY_LIGHT_GREY);
//                        content.remove(panel);
//                        content.validate();
//                    }
//                }
//            });

        }};
        _w.coordinatesPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));


        contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        _w.add(contentPanel, new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            fill = HORIZONTAL;
            weightx = 1.0;
            weighty = 2.0;
            insets = new Insets(5, 0, 5, 0);
        }});

        _w.add(_detailEditor, new GridBagConstraints() {{
            gridx = 0;
            gridy = 2;
            fill = BOTH;
            weightx = 2.0;
            weighty = 2.0;
            insets = new Insets(5, 0, 5, 0);
        }});

        // Set up the formatting on the calendar doodad
        final DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
        timeFormatter.setTimeZone(UTC);
        _w.calendarTime.setModel(new DefaultComboBoxModel<>(TimeConfig.values()));
        _w.calendarTime.setRenderer(new ListCellRenderer<TimeConfig>() {

            // This is raw-typed; we use a delegate to avoid the warning
            BasicComboBoxRenderer delegate = new BasicComboBoxRenderer() {
                public Component getListCellRendererComponent(JList jList, Object
                object, int index, boolean isSelected, boolean hasFocus) {
                    final String text;
                    if (object instanceof Date) {
                        text = timeFormatter.format((Date) object);
                    } else if (object instanceof TimeConfig) {
                        text = ((TimeConfig) object).displayValue();
                    } else {
                        text = object.toString();
                    }
                    return super.getListCellRendererComponent(jList, text, index, isSelected, hasFocus);
                }
            };

            public Component getListCellRendererComponent(JList<? extends TimeConfig> list, TimeConfig value, int index, boolean isSelected, boolean cellHasFocus) {
                return delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

        });
        _timeDocument.setTime(timeFormatter.format(new Date()));
        ((JTextField) _w.calendarTime.getEditor().getEditorComponent()).setDocument(_timeDocument);
        _w.calendarDate.setTimeZone(UTC);



        setMenuStyling(_w.newMenuBar, _w.newMenu, "eclipse/add_menu.gif");



        _w.removeButton.addActionListener(removeListener);
        _w.removeButton.setText("");
        _w.removeButton.setIcon(Resources.getIcon("eclipse/remove.gif"));
        ButtonFlattener.flatten(_w.removeButton);

        _w.copyButton.setIcon(Resources.getIcon("eclipse/copy.gif"));
        _w.copyButton.setText("");
        _w.copyButton.addActionListener(copyListener);
        ButtonFlattener.flatten(_w.copyButton);

        _w.pasteButton.setIcon(Resources.getIcon("eclipse/paste.gif"));
        _w.pasteButton.setText("");
        _w.pasteButton.addActionListener(pasteListener);
        ButtonFlattener.flatten(_w.pasteButton);

        _w.duplicateButton.setIcon(Resources.getIcon("eclipse/duplicate.gif"));
        _w.duplicateButton.setText("");
        _w.duplicateButton.addActionListener(duplicateListener);
        ButtonFlattener.flatten(_w.duplicateButton);

        _w.primaryButton.setIcon(Resources.getIcon("eclipse/radiobuttons.gif"));
        _w.primaryButton.setText("");
        _w.primaryButton.addActionListener(primaryListener);
        ButtonFlattener.flatten(_w.primaryButton);

//        _w.resolveButton.addActionListener(resolveListener);
//        _w.resolveButton.setText("");
//        _w.resolveButton.setIcon(Resources.getIcon("eclipse/search.gif"));
//        ButtonFlattener.flatten(_w.resolveButton);


        _w.autoGuideStarGuiderSelector.addSelectionListener(new AgsSelectorControl.Listener() {
            public void agsStrategyUpdated(Option<AgsStrategy> strategy) {
                AgsStrategyUtil.setSelection(getContextObservation(), strategy);
            }
        });
        _w.autoGuideStarButton.addActionListener(autoGuideStarListener);
        _w.manualGuideStarButton.addActionListener(manualGuideStarListener);


        _w.setBaseButton.addActionListener(setBaseListener);

        _w.calendarTime.addActionListener(calendarTimeListener);

//        _w.timeRangePlotButton.addActionListener(timeRangePlotListener);
//        _w.updateRaDecButton.addActionListener(updateRaDecListener);

//        setMenuStyling(_w.nameServerBar, _w.nameServer, "eclipse/menu-trimmed.gif");

//        final SkycatConfigFile cf = SkycatConfigFile.getConfigFile();

//        // Create radio button options for each of the name servers.
//        final ButtonGroup grp = new ButtonGroup();
//        final List<Catalog> nameServers = cf.getNameServers();
//        for (final Catalog ns : nameServers) {
//            final JRadioButtonMenuItem mi = new JRadioButtonMenuItem(ns.getName()) {{
//                addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                        _selectedNameServer = new Some<>(ns);
//                    }
//                });
//            }};
//            grp.add(mi);
//            _w.nameServer.add(mi);
//        }
//
//        // Set the initial name server options
//        if (nameServers.size() > 0) {
//            _selectedNameServer = new Some<>(nameServers.get(0));
//            grp.setSelected(((JMenuItem) _w.nameServer.getMenuComponent(0)).getModel(), true);
//        }

        _w.targetName.setMinimumSize(_w.targetName.getPreferredSize());
        _w.targetName.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                String name = tbwe.getText();
                if (name != null) name = name.trim();
                _curPos.deleteWatcher(posWatcher);
                _curPos.getTarget().setName(name);
                _curPos.notifyOfGenericUpdate();
                _curPos.addWatcher(posWatcher);
//                _w.resolveButton.setEnabled(!"".equals(name));
            }

            public void textBoxAction(TextBoxWidget tbwe) {
//                resolveName(HorizonsAction.Type.GET_ORBITAL_ELEMENTS, null);
            }
        });

        _w.guideGroupName.setMinimumSize(_w.guideGroupName.getPreferredSize());
        _w.guideGroupName.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final String name = _w.guideGroupName.getText();
                        final GuideGroup newGroup = _curGroup.setName(name);
                        final TargetEnvironment env = getDataObject().getTargetEnvironment();
                        final GuideEnvironment ge = env.getGuideEnvironment();
                        final ImList<GuideGroup> options = ge.getOptions();
                        final List<GuideGroup> list = new ArrayList<>(options.size());
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

        _w.xaxis.addWatcher(axisWatcher);
        _w.yaxis.addWatcher(axisWatcher);

        _w.system.clear();
        _w.system.addChoice(HmsDegTarget.TAG.tccName);
        _w.system.addChoice(NON_SIDEREAL_TARGET);
        _w.system.addWatcher(new DropDownListBoxWidgetWatcher() {
            public void dropDownListBoxAction(DropDownListBoxWidget dd, int i, String val) {
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
                refreshAll();
                updateGuiding();
            }
        });

        _nonSiderealTargetSup.initOrbitalElementFormatChoices();
        _nonSiderealTargetSup.initListeners(nonsiderealTextBoxWidgetWatcher);

        final JRadioButton[] buttons = _w.planetButtons;
        for (JRadioButton button : buttons) {
            button.addActionListener(solarListener);
        }

        // *** Position Table
        _w.positionTable.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    // Make the delete and backspace buttons delete selected positions.
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        _w.removeButton.doClick();
                        break;
                }
            }
        });

        _agsPub.subscribe(new AgsContextSubscriber() {
            @Override public void notify(ISPObservation obs, AgsContext oldOptions, AgsContext newOptions) {
                updateGuiding();
            }
        });

    }

    /**
     * Set the contained target's RA and Dec from Strings in HMS/DMS format and notify listeners.
     * Invalid values are replaced with 00:00:00.
     */
    private static void setHmsDms(SPTarget spTarget, final String hms, final String dms) {
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

    @Override protected void updateEnabledState(boolean enabled) {
        super.updateEnabledState(enabled);

        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        _w.tag.setEnabled(enabled && env.getBase() != _curPos);

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.setEnabled(enabled && inst != null);
    }

    private static void setMenuStyling(JMenuBar bar, final JMenu menu, final String iconPath) {
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setOpaque(false);
        menu.setIcon(Resources.getIcon(iconPath));
        menu.setText("");
        menu.addMouseListener(new MouseAdapter() {
            final Icon icon = Resources.getIcon(iconPath);
            @Override public void mouseEntered(MouseEvent e) {
                menu.setIcon(menu.getRolloverIcon());
            }
            @Override public void mouseExited(MouseEvent e) {
                menu.setIcon(icon);
            }
        });
        ButtonFlattener.flatten(menu);
    }

    private final ActionListener _tagListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final PositionType pt = (PositionType) _w.tag.getSelectedItem();
            pt.morphTarget(getDataObject(), _curPos);
            if (getDataObject() != null) {
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                if (env.getTargets().contains(_curPos)) {

                    final Option<ObsContext> ctx = getObsContext(env);

                    if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                    // Sidereal
//                    _siderealEditor.edit(ctx, _curPos, getNode());
//                    _trackingEditor.edit(ctx, _curPos, getNode());

                    // Nonsidereal
                    _nonSiderealTargetSup.updatePos(_curPos);
//                    _nonsideMagEditor.edit(ctx, _curPos, getNode());

                    // Detail
                    _detailEditor.edit(ctx, _curPos, getNode());

                    if (_curPos != null) {
                        _curPos.addWatcher(posWatcher);
                        refreshAll();

                        // can't remove base position, so disable button
                        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                        _w.removeButton.setEnabled(_curPos != env.getBase() && editable);
                        _w.primaryButton.setEnabled(enablePrimary(_curPos, env) && editable);
                    }
                }
            }
        }
    };

    /**
     * A renderer of target type options.  Shows a guider type that isn't available in the current
     * context with a warning icon.
     */
    private final DefaultListCellRenderer tagRenderer = new DefaultListCellRenderer() {
        private final Icon errorIcon = Resources.getIcon("eclipse/error.gif");
        @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final PositionType pt = (PositionType) value;
            if (!pt.isAvailable()) {
                lab.setFont(lab.getFont().deriveFont(Font.ITALIC));
                lab.setIcon(errorIcon);
            }
            return lab;
        }
    };

    private static final class TargetClipboard {
        private ITarget target;
        private GuideGroup group;
        private ImList<Magnitude> mag;

        static Option<TargetClipboard> copy(TargetEnvironment env, ISPObsComponent obsComponent) {
            if (obsComponent == null) return None.instance();

            final SPTarget target = TargetSelection.get(env, obsComponent);
            if (target == null) {
                final GuideGroup group = TargetSelection.getGuideGroup(env, obsComponent);
                if (group == null) {
                    return None.instance();
                }
                return new Some<>(new TargetClipboard(group));
            }
            return new Some<>(new TargetClipboard(target));
        }

        TargetClipboard(SPTarget spTarget) {
            this.target = spTarget.getTarget().clone();
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
                    spTarget.setTarget(target.clone());
                    spTarget.getTarget().setMagnitudes(mag);
                    spTarget.notifyOfGenericUpdate();
                }
            } else {
                final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
                if (group != null) {
                    if (this.group != null) {
                        final GuideGroup newGroup = group.setAll(this.group.cloneTargets().getAll());
                        // XXX TODO: add a helper method in the model to replace a guide group
                        final TargetEnvironment env = dataObject.getTargetEnvironment();
                        final GuideEnvironment ge = dataObject.getTargetEnvironment().getGuideEnvironment();
                        final ImList<GuideGroup> options = ge.getOptions();
                        final ArrayList<GuideGroup> list = new ArrayList<>(options.size());
                        for (GuideGroup gg : options) {
                            list.add(gg == group ? newGroup : gg);
                        }
                        dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                    }
                }
            }
        }
    }


//    /**
//     * Spark off an asynchronous task to look up the target in Horizons or a catalog, depending on
//     * target type. If it's a nonsidereal target, use the passed operation type and notify when
//     * complete, if listener is non-null. Otherwise do a name lookup and ignore the arguments.
//     */
//    private void resolveName(final HorizonsAction.Type operationType, final ResolveNameListener listener) {
//        if (_w.system.getSelectedItem() == NON_SIDEREAL_TARGET) {
//            scheduleHorizonsLookup(operationType, listener);
//        } else {
//            scheduleCatalogLookup();
//        }
//    }
//
//    private void scheduleCatalogLookup() {
//        if (!_selectedNameServer.isEmpty()) {
//            final Catalog nameServer = _selectedNameServer.getValue();
//            final String name  = _w.targetName.getText().trim();
//            if (name.length() != 0) {
//                final QueryArgs queryArgs = new BasicQueryArgs(nameServer);
//                queryArgs.setId(name);
//                if (nameServer != null) {
//                    new SwingWorker() {
//                        public Object construct() {
//                            try {
//                                final String simbadCatalogShortName = "simbad";
//                                String originalUrls[] = null;
//                                if (nameServer instanceof SkycatCatalog) {
//                                    final SkycatCatalog skycat = (SkycatCatalog) nameServer;
//                                    if (skycat.getShortName().contains(simbadCatalogShortName)) {
//                                        skycat.addCatalogFilter(FullMimeSimbadCatalogFilter.getFilter());
//                                        final int n = skycat.getConfigEntry().getNumURLs();
//                                        final String[] urls = new String[n];
//                                        originalUrls = new String[n];
//                                        for (int i = 0; i < n; i++) {
//                                            String urlStr = skycat.getConfigEntry().getURL(i);
//                                            originalUrls[i] = urlStr;
//                                            urls[i] = urlStr;
//                                        }
//                                        skycat.getConfigEntry().setURLs(urls);
//                                    }
//                                }
//                                queryArgs.setMaxRows(1);
//                                final QueryResult r = nameServer.query(queryArgs);
//                                //Return the URLs to the previous state
//                                if (nameServer instanceof SkycatCatalog) {
//                                    final SkycatCatalog skycat = (SkycatCatalog) nameServer;
//                                    if (skycat.getShortName().contains(simbadCatalogShortName)) {
//                                        skycat.getConfigEntry().setURLs(originalUrls);
//                                    }
//                                }
//
//                                if (r instanceof TableQueryResult) {
//                                    final TableQueryResult tqr = (TableQueryResult) r;
//                                    if (tqr.getRowCount() > 0) {
//                                        return tqr;
//                                    }
//                                    throw new CatalogException("No objects were found.");
//                                }
//                                throw new CatalogException("Error contacting catalog server");
//                            } catch (Exception e) {
//                                return e;
//                            }
//                        }
//                        public void finished() {
//                            final Object o = getValue();
//                            if (o instanceof TableQueryResult) {
//                                final TableQueryResult tqr = (TableQueryResult) o;
//                                final int pm = tqr.getColumnIndex("pm1");
//                                if (pm >= 0) {
//                                    final Double pm1 = (Double) tqr.getValueAt(0, pm);
//                                    final Double pm2 = (Double) tqr.getValueAt(0, pm + 1);
//                                    setPM(_curPos, pm1, pm2);
//                                } else {
//                                    //SCT-301: If not found, then we reset the value to zero
//                                    setPM(_curPos, 0.0, 0.0);
//                                }
//                                if (tqr.getCoordinates(0) instanceof WorldCoords) {
//                                    final WorldCoords pos = (WorldCoords) tqr.getCoordinates(0);
//                                    _w.xaxis.setText(pos.getRA().toString());
//                                    _w.yaxis.setText(pos.getDec().toString());
//                                    axisWatcher.textBoxAction(_w.yaxis); // pretend the user did this
//                                }
//                            } else if (o instanceof Exception) {
//                                DialogUtil.error(((Exception) o).getMessage());
//                            }
//                        }
//                        private void setPM(SPTarget spt, double ra, double dec) {
//                            final ITarget it = spt.getTarget();
//                            if (it instanceof HmsDegTarget) {
//                                final HmsDegTarget t = (HmsDegTarget) it;
//                                t.setPropMotionRA(ra);
//                                t.setPropMotionDec(dec);
//                            }
//                            spt.notifyOfGenericUpdate();
//                        }
//
//                    }.start();
//                }
//            }
//        }
//    }
//
//    private void scheduleHorizonsLookup(final HorizonsAction.Type operationType, final ResolveNameListener listener) {
//        final String name;
//        if (_curPos.getTarget() instanceof NamedTarget) {
//            final NamedTarget target = (NamedTarget) _curPos.getTarget();
//            name = target.getSolarObject().getHorizonsId();
//        } else {
//            name = _w.targetName.getText().trim();
//        }
//        if (!name.isEmpty()) {
//            new AuthSwingWorker<HorizonsReply, Object>() {
//                @Override
//                protected HorizonsReply doInBackgroundWithSubject() {
//
//                    //for NamedTargets we don't resolve orbital elements
//                    if (_curPos.getTarget() instanceof NamedTarget &&
//                            operationType == HorizonsAction.Type.GET_ORBITAL_ELEMENTS) {
//                        return null;
//                    }
//
//                    //ignore events that trigger an Horizon's reply cache reset
//                    _nonSiderealTargetSup.ignoreResetCacheEvents(true);
//                    //If it's a PI, we need to figure out where to set the horizons site
//                    final HorizonsService service = HorizonsService.getInstance();
//                    if (service == null) return null;
//
//                    if (!OTOptions.isStaff(getProgram().getProgramID())) {
//                        SPProgramID id = null;
//                        try {
//                            id = getProgram().getProgramID();
//                        } catch (NullPointerException e) {
//                            //this shouldn't happen
//                        }
//                        service.setSite(id);
//                    }
//
//
//                    //Get the Date to start the query
//                    //get the appropriate date to perform the query based on the calendar widget and
//                    //the time widget
//                    final Date date1 = _w.calendarDate.getDate();
//                    //this date is at 00:00:00. Let's add the hours, minutes and sec from the time document
//
//                    final Calendar calendar = Calendar.getInstance(UTC);
//                    calendar.setTimeZone(_w.calendarDate.getTimeZone());
//                    calendar.setTime(date1);
//                    calendar.set(Calendar.HOUR_OF_DAY, _timeDocument.getHoursField());
//                    calendar.set(Calendar.MINUTE, _timeDocument.getMinutesField());
//                    calendar.set(Calendar.SECOND, _timeDocument.getSecondsField());
//
//                    final Date date = calendar.getTime();
//
//                    //lets see if we can use the existing ephemeris
//                    final HorizonsReply lastReply = service.getLastResult();
//                    if (lastReply != null && name.equals(service.getObjectId())) {
//                        //so, apparently nothing has changed
//                        boolean workable = true;
//                        if (operationType == HorizonsAction.Type.GET_ORBITAL_ELEMENTS) {
//                            if (!lastReply.hasOrbitalElements()) {
//                                //ups. This reply doesn't quite work.
//                                workable = false;
//                            }
//                        }
//
//                        //let's see if we have ephemeris, and with that we are
//                        //okay (the ephemeris is usable for operations that doesn't
//                        //involve updating orbital elements.
//                        if (lastReply.hasEphemeris() && workable) {
//                            final EphemerisEntry firstEntry = lastReply.getEphemeris().get(0);
//                            if (date.equals(firstEntry.getDate())) {
//                                //Same results
//                                return lastReply;
//                            }
//                        }
//
//                    }
//
//                    service.setInitialDate(date);
//                    service.setObjectId(name);
//                    service.setObjectType(null);
//
//                    final ITarget.Tag tag = (ITarget.Tag) _w.orbitalElementFormat.getSelectedItem();
//                    switch (tag) {
//                        case JPL_MINOR_BODY:   service.setObjectType(HorizonsQuery.ObjectType.COMET);      break;
//                        case MPC_MINOR_PLANET: service.setObjectType(HorizonsQuery.ObjectType.MINOR_BODY); break;
//                        case NAMED:            service.setObjectType(HorizonsQuery.ObjectType.MAJOR_BODY); break;
//                    }
//                    _nonSiderealTargetSup.ignoreResetCacheEvents(false);
//
//                    HorizonsReply reply = service.execute();
//
//                    if (reply == null || reply.getReplyType() == HorizonsReply.ReplyType.MAJOR_PLANET) {
//                        if (service.getObjectType() == HorizonsQuery.ObjectType.COMET) {
//                            service.setObjectType(HorizonsQuery.ObjectType.MINOR_BODY);
//                            reply = service.execute();
//                        } else if (service.getObjectType() == HorizonsQuery.ObjectType.MINOR_BODY) {
//                            service.setObjectType(HorizonsQuery.ObjectType.COMET);
//                            reply = service.execute();
//                        }
//                    }
//
//                    return reply;
//                }
//
//                @Override
//                public void done() {
//                    final HorizonsReply reply;
//                    try {
//                        reply = get();
//                    } catch (InterruptedException ex) {
//                        return;
//                    } catch (ExecutionException ex) {
//                        final String message = "An error occurred fetching results for: " + name.toUpperCase();
//                        LOG.log(Level.WARNING, message, ex);
//                        DialogUtil.message(message);
//                        return;
//                    }
//
//                    if ((reply == null) || (reply.getReplyType() == HorizonsReply.ReplyType.NO_RESULTS)) {
//                        DialogUtil.message("No results were found for: " + name.toUpperCase());
//                        return;
//                    }
//
//                    if (reply.getReplyType() == HorizonsReply.ReplyType.MAJOR_PLANET
//                            && !(_curPos.getTarget() instanceof NamedTarget)) {
//                        DialogUtil.message("Can't solve the given ID to any minor body");
//                        return;
//                    }
//
//                    if (reply.getReplyType() == HorizonsReply.ReplyType.SPACECRAFT) {
//                        DialogUtil.message("Horizons suggests this is a spacecraft. Sorry, but OT can't use spacecrafts");
//                        return;
//                    }
//
//                    if (reply.getObjectType() == null) {//it will be null if you press "cancel" in the window with multiple responses
//                        return;
//                    }
//
//                    if ((_curPos.getTarget() instanceof NamedTarget) && ((NamedTarget) _curPos.getTarget()).getSolarObject() == NamedTarget.SolarObject.PLUTO) {
//                        reply.setReplyType(HorizonsReply.ReplyType.MAJOR_PLANET);
//                    }
//
//
//                    //ignore events that would reset the Horizons results cache
//                    _nonSiderealTargetSup.ignoreResetCacheEvents(true);
//                    //if a multiple answer was executed, then we need to recover the Id used
//                    final HorizonsService service = HorizonsService.getInstance();
//                    if (service != null) {
//                        final String objectId = service.getObjectId();
//                        _curPos.getTarget().setName(objectId);
//                        _curPos.notifyOfGenericUpdate();
//                    }
//
//                    final HorizonsAction action = new HorizonsActionContainer().getActions().get(operationType);
//                    try {
//                        action.execute(reply);
//                    } catch (NullPointerException ex) {
//                        final Logger LOG1 = Logger.getLogger(EdCompTargetList.class.getName());
//                        LOG1.log(Level.INFO, "Probable problem parsing the reply from JPL", ex);
//                        return;
//                    }
//                    if (_curPos.getTarget() instanceof NonSiderealTarget) {
//                        final NonSiderealTarget oldTarget = (NonSiderealTarget) _curPos.getTarget();
//                        switch (reply.getObjectType()) {
//                            case COMET: {
//                                final ConicTarget target = getOrCreateTargetWithTag(ITarget.Tag.JPL_MINOR_BODY);
//                                _nonSiderealTargetSup.showNonSiderealTarget(target);
//                                _w.orbitalElementFormat.setValue(ITarget.Tag.JPL_MINOR_BODY);
//                                _curPos.setTarget(target);
//                            }
//                            break;
//                            case MINOR_BODY: {
//                                final ConicTarget target = getOrCreateTargetWithTag(ITarget.Tag.MPC_MINOR_PLANET);
//                                _nonSiderealTargetSup.showNonSiderealTarget(target);
//                                _w.orbitalElementFormat.setValue(ITarget.Tag.MPC_MINOR_PLANET);
//                                _curPos.setTarget(target);
//                            }
//                            break;
//                            case MAJOR_BODY: {
//                                final NamedTarget target = (oldTarget instanceof NamedTarget) ? (NamedTarget) oldTarget : new NamedTarget();
//                                _nonSiderealTargetSup.showNonSiderealTarget(target);
//                                _w.orbitalElementFormat.setValue(ITarget.Tag.NAMED);
//                                _curPos.setTarget(target);
//                            }
//                            break;
//                        }
//
//                    }
//                    _nonSiderealTargetSup.ignoreResetCacheEvents(false);
//                    if (listener != null) {
//                        listener.nameResolved();
//                    }
//                }
//            }.execute();
//        }
//    }



    /**
     * Returns the current target if its system type matches the specified one, otherwise constructs
     * and returns a new conic target of the specified type.
     */
    private ConicTarget getOrCreateTargetWithTag(ITarget.Tag tag) {
        final ITarget old = _curPos.getTarget();
        if (old instanceof ConicTarget && old.getTag() == tag)
            return (ConicTarget) old;
        else
            return (ConicTarget) ITarget.forTag(tag);
    }

    // OtItemEditor
    public JPanel getWindow() {
        return _w;
    }

    // OtItemEditor
    public void init() {

        final ISPObsComponent node = getContextTargetObsComp();
        TargetSelection.listenTo(node, selectionListener);

        getDataObject().addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);

        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        final SPTarget selTarget = TargetSelection.get(env, node);
        if (getDataObject() != null) {
            final TargetEnvironment env2 = getDataObject().getTargetEnvironment();
            if (env2.getTargets().contains(selTarget)) {

                final Option<ObsContext> ctx = getObsContext(env2);

                if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                _curPos = selTarget;

                // Sidereal
//                _siderealEditor.edit(ctx, selTarget, getNode());
//                _trackingEditor.edit(ctx, selTarget, getNode());

                // Nonsidereal
                _nonSiderealTargetSup.updatePos(_curPos);
//                _nonsideMagEditor.edit(ctx, selTarget, getNode());

                if (_curPos != null) {
                    _curPos.addWatcher(posWatcher);
                    refreshAll();

                    // can't remove base position, so disable button
                    final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                    _w.removeButton.setEnabled(_curPos != env2.getBase() && editable);
                    _w.primaryButton.setEnabled(enablePrimary(selTarget, env2) && editable);
                }
            }
        }

        final TargetObsComp obsComp = getDataObject();
        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.removeAll();
        if (obsComp == null || inst == null) {
            _w.newMenu.setEnabled(false);
        } else {
            _w.newMenu.setEnabled(true);

            final TargetEnvironment env1 = obsComp.getTargetEnvironment();

            if (inst.hasGuideProbes()) {
                final List<GuideProbe> guiders = new ArrayList<>(env1.getGuideEnvironment().getActiveGuiders());
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
        _w.positionTable.reinit(getDataObject());
        _w.manualGuideStarButton.setVisible(GuideStarSupport.supportsManualGuideStarSelection(getNode()));
        updateGuiding();
        _agsPub.watch(getContextObservation());
    }

    // OtItemEditor
    protected void cleanup() {
        _agsPub.watch(null);
        TargetSelection.deafTo(getContextTargetObsComp(), selectionListener);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);
        super.cleanup();
    }

    private void toggleAgsGuiElements() {
        final boolean supports = GuideStarSupport.supportsAutoGuideStarSelection(getNode());
        _w.guidingControls.supportsAgs_$eq(supports); // hide the ags related buttons
    }

    // Guider panel property change listener to modify status and magnitude limits.
    private final PropertyChangeListener guidingPanelUpdater = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            updateGuiding((TargetEnvironment) evt.getNewValue());
        }
    };

    private Option<ObsContext> getObsContext(final TargetEnvironment env) {
        return ObsContext.create(getContextObservation()).map(new Function1<ObsContext, ObsContext>() {
            @Override public ObsContext apply(ObsContext obsContext) {
                return obsContext.withTargets(env);
            }
        });
    }

    private void updateGuiding() {
        updateGuiding(getDataObject().getTargetEnvironment());
    }

    private void updateGuiding(final TargetEnvironment env) {
        toggleAgsGuiElements();
        final Option<ObsContext> ctx = getObsContext(env);
        _w.guidingControls.update(ctx);
//        _siderealEditor.updateGuiding(ctx, _curPos, getNode());
    }

    private final PropertyChangeListener selectionListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final ISPObsComponent node = getContextTargetObsComp();
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            final SPTarget target = TargetSelection.get(env, node);
            if (target != null) {
                if (getDataObject() != null) {
                    final TargetEnvironment env1 = getDataObject().getTargetEnvironment();
                    if (env1.getTargets().contains(target)) {

                        final Option<ObsContext> ctx = getObsContext(env1);

                        if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                        _curPos = target;

                        // Sidereal
//                        _siderealEditor.edit(ctx, target, getNode());
//                        _trackingEditor.edit(ctx, target, getNode());

                        // Nonsidereal
                        _nonSiderealTargetSup.updatePos(_curPos);
//                        _nonsideMagEditor.edit(ctx, target, getNode());

                        if (_curPos != null) {
                            _curPos.addWatcher(posWatcher);
                            refreshAll();

                            // can't remove base position, so disable button
                            final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                            _w.removeButton.setEnabled(_curPos != env1.getBase() && editable);
                            _w.primaryButton.setEnabled(enablePrimary(target, env1) && editable);
                        }
                    }
                }
            } else {
                final GuideGroup grp = TargetSelection.getGuideGroup(env, node);
                if (grp != null) if (getDataObject() != null) {
                    final TargetEnvironment env1 = getDataObject().getTargetEnvironment();
                    if (env1.getGroups().contains(grp)) {

                        if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                        _curPos = null;
                        _curGroup = grp;

                        _w.objectGBW.setVisible(false);
                        _w.extrasFolder.setVisible(false);
                        _w.guideGroupPanel.setVisible(true);
//                        if (_trackingButton.isSelected())
//                            _trackingButton.doClick();
//                        _trackingButton.setVisible(false);

                        // N.B. don't trim, otherwise user can't include space in group name
                        final String name = _curGroup.getName().getOrElse("");
                        _w.guideGroupName.setValue(name);

                        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                        _w.removeButton.setEnabled(editable);
                        _w.primaryButton.setEnabled(editable);
                    }
                }
            }
        }
    };

    // Updates the enabled state of the primary guide target button when the target environment changes.
    private final PropertyChangeListener primaryButtonUpdater = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            boolean enabled = false;
            if (_curPos != null) {
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                final ImList<GuideProbeTargets> gtList = env.getOrCreatePrimaryGuideGroup().getAllContaining(_curPos);
                enabled = gtList.size() > 0;
            } else if (_curGroup != null) {
                enabled = true;
            }
            _w.primaryButton.setEnabled(enabled && OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()));
        }
    };

    // Action that handles adding a new guide star when a probe is picked from the add menu.
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
            final Option<GuideProbeTargets> opt = guideGroup == null ?
                    env.getPrimaryGuideProbeTargets(probe) :
                    guideGroup.get(probe);
            if (guideGroup == null) {
                guideGroup = ge.getPrimary().getValue();
            }

            final GuideProbeTargets targets;
            final SPTarget target = new SPTarget();
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
                    showTargetTag();
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
            final TargetEnvironment env = obsComp.getTargetEnvironment();
            GuideEnvironment ge = env.getGuideEnvironment();
            if (ge.getPrimary().isEmpty()) {
                ge = ge.setPrimary(env.getOrCreatePrimaryGuideGroup());
            }
            final GuideGroup primaryGroup = ge.getPrimary().getValue();
            final ImList<GuideGroup> options = ge.getOptions();
            final GuideGroup group = GuideGroup.create(null);
            final ImList<GuideGroup> groups = options.append(group);
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

    private void refreshAll() {
        final boolean isNonSidereal = (_curPos.getTarget() instanceof NonSiderealTarget);

        _w.extrasFolder.setVisible(true);
        _w.objectGBW.setVisible(true);
        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        _w.objectGBW.setEnabled(editable);
        _w.guideGroupPanel.setVisible(false);
//        _trackingButton.setVisible(!isNonSidereal);

        // Get all the legally available guiders in the current context.
        final Set<GuideProbe> avail = GuideProbeUtil.instance.getAvailableGuiders(getContextObservation());
        final Set<GuideProbe> guiders = new HashSet<>(avail);
        final TargetEnvironment env = getDataObject().getTargetEnvironment();

        // Get the set of guiders that are referenced but not legal in this context, if any.  Any
        // "available" guider is legal, anything left over is referenced but not really available.
        final Set<GuideProbe> illegalSet = env.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        illegalSet.removeAll(avail);

        // Determine whether the current position is one of these illegal guiders.  If so, we add
        // the guide probe to the list of choices so that this target may be selected in order to
        // change its type or delete it.
        GuideProbe illegal = null;
        for (GuideProbe guider : illegalSet) {
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (gtOpt.getValue().getOptions().contains(_curPos)) {
                illegal = guider;
                guiders.add(guider);
            }
        }

        // Sort the list of guiders.
        final List<GuideProbe> guidersList = new ArrayList<>(guiders);
        Collections.sort(guidersList, GuideProbe.KeyComparator.instance);

        // Make a list of PositionTypes that are legal in the current observation context.
        final PositionType[] ptA;
        ptA = new PositionType[2 + guiders.size()];

        int index = 0;
        ptA[index++] = BasePositionType.instance;
        for (GuideProbe guider : guidersList) {
            ptA[index++] = new GuidePositionType(guider, guider != illegal);
        }
        ptA[index] = UserPositionType.instance;

        _w.tag.removeActionListener(_tagListener);
        _w.tag.setModel(new DefaultComboBoxModel<>(ptA));
        _w.tag.setEnabled(isEnabled() && (env.getBase() != _curPos));
        _w.tag.addActionListener(_tagListener);

        _w.tag.setRenderer(tagRenderer);
        showTargetTag();
        final CardLayout cl = (CardLayout) _w.extrasFolder.getLayout();
        final String tag = isNonSidereal ? "nonsidereal" : "sidereal";
        cl.show(_w.extrasFolder, tag);

//        if (isNonSidereal) {
//            // Tracking applies to sidereal targets.  If shown, hide.
//            if (_trackingButton.isSelected()) _trackingButton.doClick();
//        }
//        _trackingButton.setVisible(!isNonSidereal);
//
//        //disable the nameServer if we are a non-sidereal target
//        _w.nameServer.setEnabled(!isNonSidereal);
//
//        //disable the resolve name for Named Targets
//        _w.resolveButton.setEnabled(!(_curPos.getTarget() instanceof NamedTarget));

        String name = _curPos.getTarget().getName();
        if (name != null) name = name.trim();
        _w.targetName.setValue(name);
//        _w.resolveButton.setEnabled(editable && !"".equals(name));

        _w.xaxis.setValue(_curPos.getTarget().getRa().toString());
        _w.yaxis.setValue(_curPos.getTarget().getDec().toString());

        if (isNonSidereal) {
            _w.system.setValue(NON_SIDEREAL_TARGET);
            _nonSiderealTargetSup.showOrbitalElementFormat();
        } else {
            _w.system.setValue(_curPos.getTarget().getTag().tccName);
        }

        // update the display in the tabs
        if (_curPos.getTarget() instanceof NonSiderealTarget) {
            final NonSiderealTarget nst = (NonSiderealTarget) _curPos.getTarget();
            _nonSiderealTargetSup.showNonSiderealTarget(nst);
        }

        // Update target details and force enabled state update for the detail editor, whose
        // structure may have changed (thus making the cached "enabled" value unreliable).
        _detailEditor.edit(getObsContext(env), _curPos, getNode());
        updateEnabledState(new Component[] { _detailEditor }, editable);

    }


    private void showTargetTag() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        for (int i = 0; i < _w.tag.getItemCount(); ++i) {
            final PositionType pt = _w.tag.getItemAt(i);
            if (pt.isMember(env, _curPos)) {
                _w.tag.removeActionListener(_tagListener);
                _w.tag.setSelectedIndex(i);
                _w.tag.addActionListener(_tagListener);
                break;
            }
        }
    }


    private final TelescopePosWatcher posWatcher = new TelescopePosWatcher() {
        public void telescopePosUpdate(WatchablePos tp) {
            if (_ignorePosUpdate)
                return;

            if (tp != _curPos) {
                // This shouldn't happen ...
                System.out.println(getClass().getName() + ": received a position " +
                        " update for a position other than the current one: " + tp);
                return;
            }
            refreshAll();
            updateGuiding();
        }
    };

//    // Used to notify listener when the resolveName() thread is done
//    private interface ResolveNameListener {
//        void nameResolved();
//    }

    /**
     * An Horizon Action encapsulates the operations to be performed based on the results of an
     * Horizons Query.
     */
    private interface HorizonsAction {
        public static enum Type {
            GET_ORBITAL_ELEMENTS,
            UPDATE_POSITION,
            PLOT_EPHEMERIS
        }

        /** Executes the given operation using the given <code>HorizonsReply</code> */
        public void execute(HorizonsReply reply);
    }

    /**
     * A container for all the Horizons Actions that will be performed based on the results of an
     * query to the Horizons System.
     * </p>
     * Would have been great that actions would have been implemented as static members, so I could
     * have done this as a set of enums with the appropriate operation. However, the actions operate
     * on non-static member. I think it's possible to create the appropriate methods to update the
     * relevant objects when the underlying EdCompTargetList changes, and use static methods
     * anyway... but there is so many other things to resolve yet that this is what we get.
     */
    private class HorizonsActionContainer {

        public HashMap<HorizonsAction.Type, HorizonsAction> getActions() {
            final HashMap<HorizonsAction.Type, HorizonsAction> actions = new HashMap<>();
            actions.put(HorizonsAction.Type.GET_ORBITAL_ELEMENTS,
                    new UpdateOrbitalElements());
            actions.put(HorizonsAction.Type.UPDATE_POSITION,
                    new UpdatePosition());
            actions.put(HorizonsAction.Type.PLOT_EPHEMERIS,
                    new PlotEphemeris());
            return actions;
        }

        /**
         * Update the Orbital Elements of the NonSidereal object. Will update the Ra,Dec (and time)
         * with the latest information gotten. Also, it will update the System Type of the conic
         * target based on the type of the answer.
         */
        private class UpdateOrbitalElements implements HorizonsAction {

            public void execute(HorizonsReply reply) {

                // Alright, we're going to replace the target if we get back a comet or minor
                // object, otherwise we do nothing. That's the old behavior.
                final ConicTarget target;
                final String name = _w.targetName.getText().trim();

                // First construct the object and set the AQ
                switch (reply.getReplyType()) {
                    case COMET:
                        target = new ConicTarget(ITarget.Tag.JPL_MINOR_BODY);
                        if (reply.hasOrbitalElements()) {
                            final OrbitalElements elements = reply.getOrbitalElements();
                            target.getAQ().setValue(elements.getValue(OrbitalElements.Name.QR));
                        }
                        _w.orbitalElementFormat.setSelectedItem(ITarget.Tag.JPL_MINOR_BODY);
                        _w.targetName.setText(name); //name is cleared if we move the element format
                        break;

                    case MINOR_OBJECT:
                        target = new ConicTarget(ITarget.Tag.MPC_MINOR_PLANET);
                        if (reply.hasOrbitalElements()) {
                            final OrbitalElements elements = reply.getOrbitalElements();
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
                    final OrbitalElements elements = reply.getOrbitalElements();
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
                    final List<EphemerisEntry> ephemeris = reply.getEphemeris();
                    _processEphemeris(ephemeris);
                }

                // And make sure there's an update to the UI. This is so terrible.
                _curPos.notifyOfGenericUpdate();

            }
        }

        /** Action to update the position of the object */
        private class UpdatePosition implements HorizonsAction {
            public void execute(HorizonsReply reply) {
                if (reply.hasEphemeris()) {
                    _processEphemeris(reply.getEphemeris());
                } else {
                    DialogUtil.error("No ephemeris available for object.");
                }

            }
        }

        /** Action to plot the ephemeris for the given object */
        private class PlotEphemeris implements HorizonsAction {
            public void execute(HorizonsReply reply) {
                if (reply.hasEphemeris()) {
                    List<EphemerisEntry> ephemeris = reply.getEphemeris();
                    //first, we need to setup the base position
                    _processEphemeris(ephemeris);
                    if (ephemeris != null) {
                        HorizonsPlotter.plot(getNode(), ephemeris);
                    }
                } else {
                    DialogUtil.error("No ephemeris available for object.");
                }
            }
        }

        /**
         * Analyzes the ephemeris, and set the current position (and date) of the Object based on
         * that information. If the TPE is available, will update its base position
         */
        private void _processEphemeris(List<EphemerisEntry> ephemeris) {
            if (ephemeris != null) {
                if (ephemeris.size() > 0) {
                    final EphemerisEntry entry = ephemeris.get(0);
                    final WorldCoords coords = entry.getCoordinates();
                    if (_curPos.getTarget() instanceof NonSiderealTarget) {
                        final NonSiderealTarget target = (NonSiderealTarget) _curPos.getTarget();
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
                }
            }
        }
    }

    private static final class TrackingButton extends JToggleButton {
        public TrackingButton() {
            super("Tracking Details");
            setUI(new RotatedButtonUI(RotatedButtonUI.Orientation.topToBottom));
            setBackground(VERY_LIGHT_GREY);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener removeListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            TargetEnvironment env = getDataObject().getTargetEnvironment();
            if (env.isBasePosition(_curPos)) {
                DialogUtil.error("You can't remove the Base Position.");
            } else if (_curPos != null) {
                env.removeTarget(_curPos);
            } else if (_curGroup != null) {
                final GuideGroup primary = env.getOrCreatePrimaryGuideGroup();
                if (_curGroup == primary) {
                    DialogUtil.error("You can't remove the primary guide group.");
                } else {
                    env = env.removeGroup(_curGroup);
                    _curGroup = primary;
                    getDataObject().setTargetEnvironment(env);
                    final SPTarget selTarget = TargetSelection.get(env, getNode());
                    if (env.getTargets().contains(selTarget)) {
                        final Option<ObsContext> ctx = getObsContext(env);
                        if (_curPos != null) _curPos.deleteWatcher(posWatcher);
                        _curPos = selTarget;
//                        _siderealEditor.edit(ctx, selTarget, getNode());
//                        _trackingEditor.edit(ctx, selTarget, getNode());
                        _nonSiderealTargetSup.updatePos(_curPos);
//                        _nonsideMagEditor.edit(ctx, selTarget, getNode());
                        if (_curPos != null) {
                            _curPos.addWatcher(posWatcher);
                            refreshAll();
                            final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                            _w.removeButton.setEnabled(_curPos != env.getBase() && editable);
                            _w.primaryButton.setEnabled(enablePrimary(selTarget, env) && editable);
                        }
                    }
                }
            }
        }
    };


    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener manualGuideStarListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            try {
                final TelescopePosEditor tpe = TpeManager.open();
                tpe.reset(getNode());
                tpe.getImageWidget().guideStarSearch(true);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener autoGuideStarListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            try {
                if (GuideStarSupport.hasGemsComponent(getNode())) {
                    final TelescopePosEditor tpe = TpeManager.open();
                    tpe.reset(getNode());
                    tpe.getImageWidget().guideStarSearch(false);
                } else {
                    // In general, we don't want to pop open the TPE just to
                    // pick a guide star.
                    AgsClient.launch(getNode(), _w);
                }
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener setBaseListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            final TelescopePosEditor tpe = TpeManager.get();
            if (tpe == null) {
                DialogUtil.message("The Position Editor must be opened for this feature to work.");
            } else {
                tpe.reset(getNode());
                final WorldCoords basePos = tpe.getImageCenterLocation();
                if (basePos == null) {
                    DialogUtil.message("Couldn't determine the image center.");
                } else {
                    final SPTarget base = env.getBase();
                    base.getTarget().getRa().setAs(basePos.getRaDeg(), CoordinateParam.Units.DEGREES);
                    base.getTarget().getDec().setAs(basePos.getDecDeg(), CoordinateParam.Units.DEGREES);
                    base.notifyOfGenericUpdate();
                }
            }
        }
    };

//    @SuppressWarnings("FieldCanBeLocal")
//    private final ActionListener resolveListener = new ActionListener() {
//        public void actionPerformed(ActionEvent evt) {
//            // REL-1063 Fix OT nonsidereal Solar System Object Horizons name resolution
//            if (_curPos.getTarget() instanceof NamedTarget) {
//                // For named objects like Moon, Saturn, etc don't get the orbital elements, just the position
//                resolveName(HorizonsAction.Type.UPDATE_POSITION, null);
//            } else {
//                resolveName(HorizonsAction.Type.GET_ORBITAL_ELEMENTS, null);
//            }
//        }
//    };
//
//    @SuppressWarnings("FieldCanBeLocal")
//    private final ActionListener timeRangePlotListener = new ActionListener() {
//        public void actionPerformed(ActionEvent evt) {
//            resolveName(HorizonsAction.Type.PLOT_EPHEMERIS, null);
//        }
//    };
//
//    @SuppressWarnings("FieldCanBeLocal")
//    private final ActionListener updateRaDecListener = new ActionListener() {
//        public void actionPerformed(ActionEvent evt) {
//            // REL-1063 Fix OT nonsidereal Solar System Object Horizons name resolution
//            if (_curPos.getTarget() instanceof NamedTarget) {
//                // For named objects like Moon, Saturn, etc don't get the orbital elements, just the position
//                resolveName(HorizonsAction.Type.UPDATE_POSITION, null);
//            } else {
//                // REL-343: Force nonsidereal target name resolution on coordinate updates
//                resolveName(HorizonsAction.Type.GET_ORBITAL_ELEMENTS, new ResolveNameListener() {
//                    public void nameResolved() {
//                        resolveName(HorizonsAction.Type.UPDATE_POSITION, null);
//                    }
//                });
//            }
//        }
//    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener duplicateListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final ISPObsComponent obsComponent = getNode();
            final TargetObsComp dataObject = getDataObject();
            if ((obsComponent == null) || (dataObject == null)) return;
            final SPTarget target = TargetSelection.get(dataObject.getTargetEnvironment(), obsComponent);
            if (target != null) {
                // Clone the target.
                final ParamSet ps = target.getParamSet(new PioXmlFactory());
                final SPTarget newTarget = new SPTarget();
                newTarget.setParamSet(ps);

                // Add it to the environment.  First we have to figure out what it is.
                TargetEnvironment env = dataObject.getTargetEnvironment();

                // See if it is a guide star and duplicate it in the correct GuideTargets list.
                boolean duplicated = false;
                env.getOrCreatePrimaryGuideGroup();
                final List<GuideGroup> groups = new ArrayList<>();
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
            } else {
                final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
                if (group != null) {
                    TargetEnvironment env = dataObject.getTargetEnvironment();
                    final List<GuideGroup> groups = new ArrayList<>();
                    groups.addAll(env.getGroups().toList());
                    groups.add(group.cloneTargets());
                    env = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
                    dataObject.setTargetEnvironment(env);
                    // save/restore tree state will leave last group closed, since there is one more, so expand it here
                    _w.positionTable.expandGroup(groups.get(groups.size() - 1));
                }
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener calendarTimeListener = new ActionListener() {
        final DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
        {
            timeFormatter.setTimeZone(UTC);
        }
        public void actionPerformed(ActionEvent evt) {
            final Object o = _w.calendarTime.getSelectedItem();
            if (o instanceof TimeConfig) {
                final TimeConfig tr = (TimeConfig) o;
                final Date d = tr.getDate();
                final String time = timeFormatter.format(d);
                _timeDocument.setTime(time);

                // We have to set the correct day in the calendar when shortcuts are used. Because
                // _w.calendarDate.setDate(d) doesn't work.
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
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener solarListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            final String cmd = evt.getActionCommand().toUpperCase();
            if (_curPos.getTarget() instanceof NamedTarget) {
                final NamedTarget target = (NamedTarget) _curPos.getTarget();
                try {
                    target.setSolarObject(NamedTarget.SolarObject.valueOf(cmd));
                    _curPos.notifyOfGenericUpdate();
                } catch (IllegalArgumentException ex) {
                    DialogUtil.error("Couldn't find a Planet called " + cmd);
                }
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener copyListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final Option<TargetClipboard> opt = TargetClipboard.copy(getDataObject().getTargetEnvironment(), getNode());
            if (opt.isEmpty()) return;
            clipboard = opt.getValue();
        }
    };


    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener pasteListener = new ActionListener() {
        private void pasteSelectedPosition(ISPObsComponent obsComponent, TargetObsComp dataObject) {
            if (clipboard != null) {
                clipboard.paste(obsComponent, dataObject);
            }
        }
        public void actionPerformed(ActionEvent e) {
            if (_curPos != null) {
                pasteSelectedPosition(getNode(), getDataObject());
                _curPos.notifyOfGenericUpdate();
            } else if (_curGroup != null) {
                pasteSelectedPosition(getNode(), getDataObject());
            }
        }
    };


    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener primaryListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            _w.positionTable.updatePrimaryStar();
        }
    };


    private final TextBoxWidgetWatcher axisWatcher = new TextBoxWidgetWatcher() {
        private void updateTargetCoordinatesFromTextWidgets() {
            final String dec = _w.yaxis.getText().trim().replace(",", "."); // allow "," instead of "."
            final String ra  = _w.xaxis.getText().trim().replace(",", ".");
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
        public void textBoxKeyPress(TextBoxWidget tbwe) {
            updateTargetCoordinatesFromTextWidgets();
        }
        public void textBoxAction(TextBoxWidget tbwe) {
            updateTargetCoordinatesFromTextWidgets();
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final TextBoxWidgetWatcher nonsiderealTextBoxWidgetWatcher = new TextBoxWidgetWatcher() {
        private void updateConicTarget(NumberBoxWidget nbw) {
            final ConicTarget target = (ConicTarget) _curPos.getTarget();
            _ignorePosUpdate = true;
            try {
                _nonSiderealTargetSup.setConicPos(target, nbw);
                _curPos.notifyOfGenericUpdate();
                updateGuiding();
            } finally {
                _ignorePosUpdate = false;
            }
        }
        public void textBoxKeyPress(TextBoxWidget tbw) {
            updateConicTarget((NumberBoxWidget) tbw);
        }
        public void textBoxAction(TextBoxWidget tbw) {
            updateConicTarget((NumberBoxWidget) tbw);
        }
    };

}




interface PositionType {
    boolean isAvailable();
    void morphTarget(TargetObsComp obsComp, SPTarget target);
    boolean isMember(TargetEnvironment env, SPTarget target);
}

enum BasePositionType implements PositionType {
    instance;

    public boolean isAvailable() {
        return true;
    }

    public void morphTarget(TargetObsComp obsComp, SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        final SPTarget base = env.getBase();

        final GuideEnvironment genv = env.getGuideEnvironment();
        final ImList<SPTarget> user = env.getUserTargets().append(base);

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

class GuidePositionType implements PositionType {
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
        final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
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
            final Option<GuideProbeTargets> gtOpt = group.get(guider);
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

enum UserPositionType implements PositionType {
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


