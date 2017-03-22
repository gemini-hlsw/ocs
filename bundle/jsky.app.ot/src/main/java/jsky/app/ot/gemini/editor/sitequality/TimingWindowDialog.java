package jsky.app.ot.gemini.editor.sitequality;

import edu.gemini.shared.util.HourMinuteFormat;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;

class TimingWindowDialog extends JDialog {
    private static final long MS_PER_MINUTE = 1000 * 60;
    private static final long MS_PER_HOUR   = MS_PER_MINUTE * 60;

    private static final String INSTRUCTIONS =
        "Specify a timing window. Starting time should be in the form YYYY-MM-DD hh:mm:ss " +
        "and will be interpreted in UTC. Window duration and repeat period are specified in " +
        "hours and minutes.";

    private static final SimpleDateFormat UTC   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") {{
        setTimeZone(TimeZone.getTimeZone("UTC"));
    }};
    private static final NumberFormat     TIMES = new DecimalFormat("######");


    private final JFormattedTextField window;
    private final JFormattedTextField duration;
    private final JFormattedTextField period;
    private final JFormattedTextField times;
    private final ImList<JFormattedTextField> fields;

    private final JRadioButton durationFixed;
    private final JRadioButton durationForever;

    private final JCheckBox repeatEnabled;

    private final JRadioButton repeatTypeForever;
    private final JRadioButton repeatTypeFixed;

    private final ImList<JLabel> repeatLabels;

    private final JButton okButton;
    private final JButton cancelButton;

    // Indicates if this dialog was cancelled.
    private boolean cancelled;


    TimingWindowDialog(final Frame owner) throws HeadlessException {
        super(owner, "Edit Timing Window", true);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        cancelled = true;

        // Initialize components.
        window = new JFormattedTextField(UTC);
        window.setValue(new Date());

        duration = new JFormattedTextField(HourMinuteFormat.HH_MM);
        duration.setValue(MS_PER_HOUR * 24);

        period = new JFormattedTextField(HourMinuteFormat.HH_MM_SS);
        period.setColumns(8);
        period.setValue(MS_PER_HOUR * 48);

        times = new JFormattedTextField(TIMES);
        times.setColumns(5);
        times.setValue(1000);

        fields = DefaultImList.create(window, duration, times, period);
        fields.foreach(field -> {
            field.setBorder(BorderFactory.createCompoundBorder(field.getBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    updateEnabledState();
                }
            });

            // Whenever one of the fields loses focus, its contents are automatically set to the last valid edit,
            // so the OK button should be enabled.
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    okButton.setEnabled(true);
                }
            });
        });

        durationFixed = new JRadioButton("for");
        durationForever = new JRadioButton("forever.");
        final ButtonGroup durationGroup = new ButtonGroup();
        durationGroup.add(durationFixed);
        durationGroup.add(durationForever);
        durationFixed.setSelected(true);

        repeatEnabled = new JCheckBox("The window repeats");

        repeatTypeForever = new JRadioButton();
        repeatTypeFixed   = new JRadioButton();
        final ButtonGroup repeatTypeGroup = new ButtonGroup();
        repeatTypeGroup.add(repeatTypeForever);
        repeatTypeGroup.add(repeatTypeFixed);
        repeatTypeForever.setSelected(true);

        final JToggleButton[] toggles = { repeatEnabled, repeatTypeForever, repeatTypeFixed, durationFixed, durationForever };
        for (final JToggleButton tb: toggles)
            tb.addActionListener( e -> updateEnabledState());

        repeatLabels = DefaultImList.create(
                new JLabel("forever"),
                new JLabel(" times"),
                new JLabel("with a period of ", SwingConstants.RIGHT),
                new JLabel(" (hhh:mm:ss).")
        );

        okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            cancelled = false;
            TimingWindowDialog.this.setVisible(false);
        });
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            cancelled = true;
            TimingWindowDialog.this.setVisible(false);
        });

        setContentPane(new JPanel(new GridBagLayout()) {{

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 1, 1, 1, Color.GRAY),
                    BorderFactory.createEmptyBorder(20, 20, 10, 10)));

            add(new JTextArea(INSTRUCTIONS) {{
                setWrapStyleWord(true);
                setLineWrap(true);
                setBorder(
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                            BorderFactory.createEmptyBorder(0, 0, 10, 0)));
                setOpaque(false);
                Font f = getFont();
                setFont(f.deriveFont(f.getSize2D() - 2.0f));
                setEnabled(false);
                setDisabledTextColor(Color.BLACK);
                getPreferredSize();
                setPreferredSize(new Dimension(0, 50));
            }}, gbc(0, -1, 6, new Insets(0, 0, 10, 0)));

            add(new JLabel("Observing window begins at "), gbc(0, 0, 3));
            add(window, gbc(3, 0, 3, new Insets(0, 0, 5, 10)));
            add(new JLabel("and remains open ", SwingConstants.RIGHT), gbc(0, 1, 3));

            add(durationFixed, gbc(3, 1, 1));

            add(duration, gbc(4, 1, 1));
            add(new JLabel(" (hh:mm)."), gbc(5, 1, 1));

            add(durationForever, gbc(3, 2, 3));

            add(repeatEnabled, gbc(0, 3, 4, new Insets(10, 5, 0, 0)));
            add(repeatTypeForever, gbc(0, 4, 1, new Insets(0, 20, 0, 0)));
            add(repeatLabels.get(0), gbc(1, 4, 2));
            add(repeatTypeFixed, gbc(0, 5, 1, new Insets(0, 20, 0, 0)));
            add(times, gbc(1, 5, 1));
            add(repeatLabels.get(1), gbc(2, 5, 2));
            add(repeatLabels.get(2), gbc(1, 6, 2));
            add(period, gbc(3, 6, 1));
            add(repeatLabels.get(3), gbc(4, 6, 1));

            add(new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)) {{
                setBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(10, 0, 0, 0)));
                add(okButton);
                add(cancelButton);
            }}, gbc(0, 7, 6, new Insets(20, 0, 0, 0)));
        }});

        updateEnabledState();
        pack();
    }

    private static GridBagConstraints gbc(final int x, final int y, final int xs) {
        return gbc(x, y, xs, new Insets(0, 0, 0, 0));
    }

    private static GridBagConstraints gbc(final int x, final int y, final int xs, final Insets insets) {
        return new GridBagConstraints(x, y + 1, xs, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0);
    }

    // Determine the state of components.
    private void updateEnabledState() {
        final boolean fixedDuration = durationFixed.isSelected();

        // Duration and repeat are enabled iff durationFixed is selected.
        duration.setEnabled(fixedDuration);
        repeatEnabled.setEnabled(fixedDuration);

        // Most repeat options are enabled iff repeatEnabled is selected.
        final boolean repeatAllowed = fixedDuration && repeatEnabled.isSelected();
        for (final JLabel label: repeatLabels)
            label.setEnabled(repeatAllowed);
        repeatTypeForever.setEnabled(repeatAllowed);
        repeatTypeFixed.setEnabled(repeatAllowed);
        times.setEnabled(repeatAllowed && repeatTypeFixed.isSelected());
        period.setEnabled(repeatAllowed);

        // The Ok button is enabled if all the enabled text fields are correct.
        final boolean valid = fields.filter(JFormattedTextField::isEnabled).forall(JFormattedTextField::isEditValid);
        okButton.setEnabled(valid);
    }

    // Use the dialog to create a new timing window.
    Option<TimingWindow> openNew() {
        return openEdit(new TimingWindow());
    }

    // Use the dialog to edit an existing timing window.
    Option<TimingWindow> openEdit(final TimingWindow tw) {
        setValue(tw);
        updateEnabledState();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return cancelled ? None.instance() : new Some<>(getValue());
    }

    // Initialize the fields from the supplied timing window.
    private void setValue(final TimingWindow initialValue) {
        window.setValue(new Date(initialValue.getStart()));

        if (initialValue.getDuration() == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) {
            durationForever.setSelected(true);
        } else {
            duration.setValue(initialValue.getDuration());
        }

        switch (initialValue.getRepeat()) {
            case TimingWindow.REPEAT_FOREVER:
                repeatEnabled.setSelected(true);
                repeatTypeForever.setSelected(true);
                period.setValue(initialValue.getPeriod());
                break;

            case TimingWindow.REPEAT_NEVER:
                repeatEnabled.setSelected(false);
                break;

            default:
                repeatEnabled.setSelected(true);
                repeatTypeFixed.setSelected(true);
                times.setValue(initialValue.getRepeat());
                period.setValue(initialValue.getPeriod());
                break;
        }
    }

    // Retrieve a new timing window initialized to the values in this dialog.
    private TimingWindow getValue() {
        final long start = ((Date) window.getValue()).getTime();
        if (durationForever.isSelected()) {
            return new TimingWindow(start, TimingWindow.WINDOW_REMAINS_OPEN_FOREVER, TimingWindow.REPEAT_NEVER, 0);
        }

        final long duration = (long) this.duration.getValue();
        if (repeatEnabled.isSelected()) {
            final long period = (long) this.period.getValue();
            if (repeatTypeFixed.isSelected()) {
                final int repeat = ((Number) times.getValue()).intValue();
                return new TimingWindow(start, duration, repeat, period);
            } else {
                return new TimingWindow(start, duration, TimingWindow.REPEAT_FOREVER, period);
            }
        } else {
            return new TimingWindow(start, duration, TimingWindow.REPEAT_NEVER, 0);
        }
    }
}