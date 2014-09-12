package jsky.app.ot.gemini.parallacticangle;

import edu.gemini.mask.Resources;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.inst.ParallacticAngleDuration;
import edu.gemini.spModel.inst.ParallacticAngleDurationMode;
import edu.gemini.spModel.inst.ParallacticAngleSupport;
import edu.gemini.spModel.inst.PositionAngleMode;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObsTargetCalculatorService;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.util.skycalc.calc.TargetCalculator;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.gemini.parallacticangle.ParallacticAngleDialog;
import jsky.app.ot.tpe.AgsClient;
import jsky.app.ot.util.TimeZonePreference;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


final public class ParallacticAnglePanel<T extends SPInstObsComp & ParallacticAngleSupport,
        E extends OtItemEditor<ISPObsComponent, T> & ParallacticInstEditor> extends JPanel {
    private final JCheckBox              parallacticAngleCheckBox;
    private final JMenu                  relativeTimeMenu;
    private final JButton                setDateTimeButton;
    private final JLabel                 parallacticAngleCalculation;

    /**
     * We need to type the editor as an OTItemEditor<ISPObsComponent, T> & ParallacticInstEditor (where T is the
     * instrument configuration being edited, e.g. InstGmosCommon, InstGNIRS, Flamingos2) in order to support
     * both GMOS / GNIRS editors (which inherit from EdCompInstBase) and Flamingos2 (which inherits from
     * ComponentEditor), since this is the common superclass of the different implementations.
     *
     * The EdCompInstBase editors use a TextBoxWidget (inherits from JFormattedTextField) for position angle, and
     * Flamingos2 uses a TextFieldPropertyCtrl, which encapsulates a JTextField. Since this class needs access to
     * the JTextField, we use the ParallacticInstEditor interface to get this.
     */
    private E editor;

    // We need to store the ActionListener for the parallacticAngleCheckBox so that we can add and remove it when
    // programmatically checking / unchecking the box.
    private final ActionListener         parallacticAngleCheckBoxListener;

    public ParallacticAnglePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));

        // The label should change to reflect the actual angle when selected.
        parallacticAngleCheckBox = new JCheckBox("Use Average Parallactic Angle");
        parallacticAngleCheckBox.setSelected(false);

        // Create the menu for the relative times.
        final JMenuBar bar = new JMenuBar();
        relativeTimeMenu = new JMenu();
        relativeTimeMenu.setText("Set To:");
        relativeTimeMenu.setHorizontalTextPosition(SwingConstants.LEFT);
        relativeTimeMenu.setHorizontalAlignment(SwingConstants.LEFT);
        relativeTimeMenu.setIconTextGap(10);
        final Icon icon = Resources.getIcon("eclipse/menu-trimmed.gif");
        relativeTimeMenu.setIcon(icon);
        bar.add(relativeTimeMenu);
        bar.setMinimumSize(bar.getPreferredSize());

        final Icon calendarIcon = new ImageIcon(this.getClass().getResource("dates.gif"));
        setDateTimeButton = new JButton(calendarIcon);
        setDateTimeButton.setToolTipText("Select the time and duration for the average parallactic angle calculation.");

        parallacticAngleCalculation = new JLabel(" ") {{
            setForeground(Color.black);
            setHorizontalAlignment(LEFT);
            setIconTextGap(getIconTextGap() - 2);
        }};

        // Add the components to this panel.
        add(parallacticAngleCheckBox, new GridBagConstraints() {{
            gridx = 0;
            gridy = 0;
            anchor = WEST;
            fill = HORIZONTAL;
        }});
        add(bar, new GridBagConstraints() {{
            gridx = 1;
            gridy = 0;
            anchor = CENTER;
            fill = BOTH;
            insets = new Insets(0, 10, 0, 0);
        }});
        add(setDateTimeButton, new GridBagConstraints() {{
            gridx = 2;
            gridy = 0;
            anchor = WEST;
            insets = new Insets(0, 10, 0, 0);
        }});
        add(parallacticAngleCalculation, new GridBagConstraints() {{
            gridx = 0;
            gridy = 1;
            gridwidth = 3;
            anchor = WEST;
            fill = HORIZONTAL;
            insets = new Insets(10, 0, 0, 0);  // right inset to align with "Use Average ..." text
        }});

        // TODO: Remove this and handle elsewhere.
        // Add a blank panel to absorb the rest of the vertical space.
        add(new JPanel(), new GridBagConstraints() {{
            gridx = 0;
            gridy = 2;
            weighty = 1.0;
            fill = BOTH;
        }});

        // Action listener for the parallactic angle button. We need to remove and readd this later.
        parallacticAngleCheckBoxListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (parallacticAngleCheckBox.isSelected()) {
                    editor.getDataObject().setPositionAngleMode(PositionAngleMode.MEAN_PARALLACTIC_ANGLE);

                    // If there is no scheduling block set, then set a sensible default.
                    //ISPObservation ispObservation = editor.getContextObservation();
                    //SPObservation spObservation   = (SPObservation) ispObservation.getDataObject();
                    //if (!spObservation.getSchedulingBlock().isEmpty()) {
                        displayParallacticAngle();
                    //}
                } else {
                    editor.getDataObject().setPositionAngleMode(PositionAngleMode.EXPLICITLY_SET);
                    displayParallacticAngle();
                }
            }
        };
    }

    public void init(final E editor, final Site site) {
        this.editor = editor;

        // Register the listeners that depend on the editor.
        for (ActionListener l : parallacticAngleCheckBox.getActionListeners())
            parallacticAngleCheckBox.removeActionListener(l);
        parallacticAngleCheckBox.addActionListener(parallacticAngleCheckBoxListener);

        for (ActionListener l : setDateTimeButton.getActionListeners())
            setDateTimeButton.removeActionListener(l);
        setDateTimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // We need to show the Date and Time dialog.
                // Try to get the scheduling block if one is set to initialize the date and time.
                //ISPObservation ispObservation = getContextObservation();
                //SPObservation spObservation = (SPObservation) ispObservation.getDataObject();
                //Option<SchedulingBlock> schedulingBlockOption = spObservation.getSchedulingBlock();

                final ParallacticAngleDialog dialog = new ParallacticAngleDialog(
                        editor.getViewer().getParentFrame(),
                        editor.getContextObservation(),
                        editor.getDataObject().getParallacticAngleDuration(),
                        site);
                dialog.pack();

                // This will block on the modal dialog.
                dialog.visible_$eq(true);

                // Get the return values and process accordingly.
                if (dialog.startTime().isDefined()) {
                    long startDate = ((Long) dialog.startTime().get());
                    editor.getDataObject().setParallacticAngleDuration(dialog.duration());
                    updateSchedulingBlock(startDate);
                }
            }
        });

        // Add a document listener to detect content changes to the pos angle field.
        editor.getPosAngleTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e)  { checkPosAngleAndParallacticAngle(); }

            @Override
            public void removeUpdate(DocumentEvent e)  { checkPosAngleAndParallacticAngle(); }

            @Override
            public void changedUpdate(DocumentEvent e) { }
        });

        // Setup the relative times.
        rebuildRelativeTimeMenu();
    }

    // Call to update the enabled state of the parallactic angle components.
    // This should be called by the updatedEnabledState method in the EdCompInst~~~ class that manages this widget.
    public void updateEnabledState(boolean enabled) {
        // Determine if we should allow the average parallactic angle to be used.
        // First the state in general must be enabled and the current instrument configuration should be compatible
        // with this mode (i.e. not using imaging or custom mak). Then, if already selected, leave it active
        // regardless, but if not, activate the choice only for non day-cal obs.
        final boolean compatible = enabled && editor.getDataObject().isCompatibleWithMeanParallacticAngleMode();
        final boolean useAvgPar  = compatible &&
                ((editor.getDataObject().getPositionAngleMode() == PositionAngleMode.MEAN_PARALLACTIC_ANGLE) ||         // if already selected, leave it enabled
                        (ObsClassService.lookupObsClass(editor.getContextObservation()) != ObsClass.DAY_CAL));          // if not a day-cal enable the option
        parallacticAngleCheckBox.setEnabled(useAvgPar);

        // Call the display method to set up the remainder of the components.
        displayParallacticAngle();
    }

    /**
     * Should be called every time some relevant property changes in the editor that might affect the parallactic
     * angle computation.
     */
    public void updateParallacticAngleMode() {
        T dataObject = editor.getDataObject();
        PositionAngleMode positionAngleMode = dataObject.getPositionAngleMode();
        boolean selected = positionAngleMode == PositionAngleMode.MEAN_PARALLACTIC_ANGLE;
        parallacticAngleCheckBox.removeActionListener(parallacticAngleCheckBoxListener);
        parallacticAngleCheckBox.setSelected(selected);
        parallacticAngleCheckBox.addActionListener(parallacticAngleCheckBoxListener);

        parallacticAngleCheckBox.setEnabled(dataObject.isCompatibleWithMeanParallacticAngleMode());

        // Now trigger the initialization of the parallactic angle GUI components and calculation.
        displayParallacticAngle();
    }

    // Update the scheduling block and trigger the parallactic angle to redisplay.
    // Duration should be in milliseconds at this point!
    private void updateSchedulingBlock(long start) {
        final ISPObservation ispObservation = editor.getContextObservation();
        final SPObservation spObservation   = (SPObservation) ispObservation.getDataObject();

        // Calculate the duration.
        // Create a new scheduling block with now as the start time, and the remaining exec time as the duration.
        long remainingTime = ParallacticAngleDuration.calculateRemainingTime(ispObservation);
        ParallacticAngleDuration parallacticAngleDuration = editor.getDataObject().getParallacticAngleDuration();
        long duration = parallacticAngleDuration.getParallacticAngleDurationMode() == ParallacticAngleDurationMode.EXPLICITLY_SET ?
                parallacticAngleDuration.getExplicitDuration() : remainingTime;
        spObservation.setSchedulingBlock(new Some<SchedulingBlock>(new SchedulingBlock(start, duration)));
        ispObservation.setDataObject(spObservation);
        setPosAngleToParallacticAngle();
        displayParallacticAngle();
    }

    private static double angleDegrees(Angle a) {
        return a.toPositive().toDegrees().getMagnitude();
    }

    private void checkPosAngleAndParallacticAngle() {
        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(2);

        final Option<Angle> parAngleOption = getParallacticAngle();

        // Determine if, for all intents and purposes, the two are equal (to 0.1 deg).
        // Before we compared numerically, i.e.: Math.abs(posAngle - parAngleOption.getValue()) < 5e-3)
        Icon i = Resources.getIcon("eclipse/blank.gif");
        if (parAngleOption.isEmpty()) {
            final ISPObservation ispObservation              = editor.getContextObservation();
            final SPObservation spObservation                = (SPObservation) ispObservation.getDataObject();
            final Option<SchedulingBlock> schedulingBlockOpt = spObservation.getSchedulingBlock();
            if (!schedulingBlockOpt.isEmpty()) {
                // Target not visible, show alert.
                i = Resources.getIcon("eclipse/alert.gif");
            }
        } else {
            final Angle parAngle      = parAngleOption.getValue();
            final String posAngleText = editor.getPosAngleTextField().getText().trim();
            if (!nf.format(angleDegrees(parAngle)).equals(posAngleText) &&
                    !nf.format(angleDegrees(parAngle.add(Angle.ANGLE_PI))).equals(posAngleText)) {
                // Pos angle doesn't match par angle, show alert.
                i = Resources.getIcon("eclipse/alert.gif");
            }
        }
        parallacticAngleCalculation.setIcon(i);
    }

    private void setPosAngleToParallacticAngle() {
        Option<Angle> angle = getParallacticAngle();
        if (!angle.isEmpty()) {
            // Set it in the data object. This triggers the property change in EdCompInstBase.
            T dataObject = editor.getDataObject();
            dataObject.setPosAngle(angleDegrees(angle.getValue()));

            // TODO: Do we want to do this here??? Possibility: add an agsLaunch method to editor.
            // Trigger the AGS lookup.
            AgsClient.launch(editor.getNode(), editor.getWindow());

            // Make the parallactic angle calculation black as it is not equal to pos angle.
            parallacticAngleCalculation.setForeground(Color.black);
        }
    }

    // Get the value of the parallactic angle calculation.
    private Option<Angle> getParallacticAngle() {
        final ISPObservation ispObservation = editor.getContextObservation();
        return editor.getDataObject().calculateParallacticAngle(ispObservation);
    }

    // Determine if a target calculator already exists for the observation.
    private boolean hasTargetCalculator() {
        final ISPObservation ispObservation                         = editor.getContextObservation();
        final Option<TargetCalculator> targetCalculatorOption = ObsTargetCalculatorService.targetCalculationForJava(ispObservation);
        return !targetCalculatorOption.isEmpty();
    }

    /**
     * This should be called whenever there is a change to any of the parallactic angle components.
     * It modifies component visibility and performs any recalculations as necessary.
     */
    private void displayParallacticAngle() {
        // Determine if we are showing the parallactic angle.
        boolean showingAngle = parallacticAngleCheckBox.isSelected();

        // This determines whether or not the calculation is visible, and the components enabled.
        parallacticAngleCalculation.setText(" ");
        parallacticAngleCalculation.setIcon(null);
        setDateTimeButton.setEnabled(showingAngle);
        relativeTimeMenu.setEnabled(showingAngle);

        // At this point, if we are not showing the angle, there is nothing more to do.
        if (!showingAngle) return;

        // Otherwise, we have to calculate the angle.
        if (hasTargetCalculator()) {
            final ISPObservation ispObservation              = editor.getContextObservation();
            final SPObservation spObservation                = (SPObservation) ispObservation.getDataObject();
            final Option<SchedulingBlock> schedulingBlockOpt = spObservation.getSchedulingBlock();

            if (!schedulingBlockOpt.isEmpty()) {
                final SchedulingBlock sb  = schedulingBlockOpt.getValue();
                final DateFormat dateF    = new SimpleDateFormat("MM/dd/yy 'at' HH:mm:ss z");
                dateF.setTimeZone(TimeZonePreference.get());
                final String dateTimeStr  = dateF.format(new Date(sb.start()));
                final double duration     = sb.duration() / 60000.0;

                // Include tenths of a minute if not even
                final String fmt = (Math.round(duration * 10) == (long) Math.floor(duration) * 10) ? "%.0f" : "%.1f";
                final String durationStr = String.format(fmt, duration);
                final String plural = "1".equals(durationStr) ? "" : "s";

                final String when = String.format("(%s, for %s min%s)", dateTimeStr, durationStr, plural);

                final Option<Angle> angle = getParallacticAngle();
                if (!angle.isEmpty()) {
                    NumberFormat nf = NumberFormat.getInstance(Locale.US);
                    nf.setMaximumFractionDigits(2);
                    final String angleText = String.format("%s\u00b0 %s", nf.format(angleDegrees(angle.getValue())), when);
                    parallacticAngleCalculation.setText(angleText);
                } else {
                    parallacticAngleCalculation.setText("Target not visible " + when);
                }
                checkPosAngleAndParallacticAngle();
            }
        }
    }

    /**
     * Rebuilds the relative time menu. Should be called any time a property might have changed in the editor that
     * affects the setup time or acquisition time.
     */
    public void rebuildRelativeTimeMenu() {
        ISPObservation ispObservation = editor.getContextObservation();
        T dataObject                  = editor.getDataObject();
        double setupTime              = dataObject.getSetupTime(ispObservation) / 60;
        double reacquisitionTime      = dataObject.getReacquisitionTime(ispObservation) / 60;

        RelativeTimeItem[] items = new RelativeTimeItem[] {
                new RelativeTimeItem("Now + Setup (" + ((int) setupTime) + " min)", setupTime),
                new RelativeTimeItem("Now + Reacq. (" + ((int) reacquisitionTime) + " min)", reacquisitionTime),
                new RelativeTimeItem("Now", 0),
                new RelativeTimeItem("Now + 10 min", 10),
                new RelativeTimeItem("Now + 20 min", 20),
                new RelativeTimeItem("Now + 30 min", 30),
                new RelativeTimeItem("Now + 45 min", 45),
                new RelativeTimeItem("Now + 60 min", 60)
        };

        ActionListener relativeTimeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RelativeTimeItem relativeTimeItem = (RelativeTimeItem) e.getSource();
                updateSchedulingBlock(new Date().getTime() + relativeTimeItem.getTimeInMs());
            }
        };

        // Clear and rebuild the menu.
        relativeTimeMenu.removeAll();
        for (RelativeTimeItem item : items) {
            relativeTimeMenu.add(item);
            item.addActionListener(relativeTimeListener);
        }
    }

    private static final class RelativeTimeItem extends JMenuItem {
        private final double minutes;

        public RelativeTimeItem(String name, double pminutes) {
            super(name);
            minutes = pminutes;
        }

        public double getMinutes() {
            return minutes;
        }
        public long getTimeInMs() {
            return (long) (minutes * 60 * 1000);
        }
    }
}
