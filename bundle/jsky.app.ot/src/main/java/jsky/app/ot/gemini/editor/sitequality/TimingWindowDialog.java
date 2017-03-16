package jsky.app.ot.gemini.editor.sitequality;

import edu.gemini.shared.util.HourMinuteFormat;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;

/**
 * Editor for a TimingWindow object. New it up and then call showEdit().
 */

public class TimingWindowDialog extends JDialog  {

    private static final long MS_PER_MINUTE = 1000 * 60;
    private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

    private static String INSTRUCTIONS =
        "Specify a timing window. Starting time should be in the form YYYY-MM-DD hh:mm:ss " +
        "and will be interpreted in UTC. Window duration and repeat period are specified in " +
        "hours and minutes.";

    ///
    /// SET UP TEXT FIELDS
    ///

    private static final SimpleDateFormat UTC     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final NumberFormat     TIMES = new DecimalFormat("######");
    static {
        UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    JFormattedTextField window   = new JFormattedTextField(UTC);
    JFormattedTextField duration = new JFormattedTextField(HourMinuteFormat.HH_MM);
    JFormattedTextField period   = new JFormattedTextField(HourMinuteFormat.HH_MM_SS) {{ setColumns(8); }};
    JFormattedTextField times      = new JFormattedTextField(TIMES)                   {{ setColumns(5); }};

    JFormattedTextField[] fields = { window, duration, times, period };

    {
        window.setValue(new Date());
        duration.setValue(MS_PER_HOUR * 24);
        times.setValue(1000);
        period.setValue(MS_PER_HOUR * 48);

        for (JFormattedTextField tf: fields) {
            tf.setBorder(BorderFactory.createCompoundBorder(tf.getBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
            tf.addKeyListener(new KeyListener() {
                @Override public void keyTyped(KeyEvent e)   {}
                @Override public void keyPressed(KeyEvent e) {}
                @Override public void keyReleased(KeyEvent e){ updateEnabledState(); }
            });
        }
    }

    ///
    /// SET UP TOGGLE BUTTONS
    ///

    JRadioButton durationFixed = new JRadioButton("for");
    JRadioButton durationForever = new JRadioButton("forever.");

    ButtonGroup durationGroup = new ButtonGroup();
    {
        durationGroup.add(durationFixed);
        durationGroup.add(durationForever);
    }

    JCheckBox repeatEnabled = new JCheckBox("The window repeats");

    JRadioButton repeatTypeForever = new JRadioButton();
    JRadioButton repeatTypeFixed = new JRadioButton();


    ButtonGroup repeatTypeGroup = new ButtonGroup();
    {
        repeatTypeGroup.add(repeatTypeForever);
        repeatTypeGroup.add(repeatTypeFixed);
    }


    {

        repeatTypeForever.setSelected(true);
        durationFixed.setSelected(true);
        JToggleButton[] toggles = { repeatEnabled, repeatTypeForever, repeatTypeFixed, durationFixed, durationForever };
        for (JToggleButton tb: toggles)
            tb.addActionListener( e -> updateEnabledState());

    }


    ///
    /// SET UP LABELS
    ///

    JLabel[] labels = new JLabel[4];


    ///
    /// SET UP BUTTONS
    ///

    boolean cancelled = true;

    JButton ok = new JButton("Ok") {{
        addActionListener(e -> {
            cancelled = false;
            TimingWindowDialog.this.setVisible(false);
        });
    }};

    JButton cancel = new JButton("Cancel") {{
        addActionListener(e -> {
            cancelled = true;
            TimingWindowDialog.this.setVisible(false);
        });
    }};


    ///
    /// FINALLY, AN INSTANCE METHOD!
    ///

    private void updateEnabledState() {

        // Repeat is enabled only if durationFixed is selected
        repeatEnabled.setEnabled(durationFixed.isSelected());

        // Most repeat options are enabled only if repeatEnabled is selected.
        boolean repeat = repeatEnabled.isEnabled() && repeatEnabled.isSelected();
        for (JLabel label: labels) label.setEnabled(repeat);
        repeatTypeForever.setEnabled(repeat);
        repeatTypeFixed.setEnabled(repeat);
        period.setEnabled(repeat);

        // With the exception of repeat count, which is enabled only if repeatEnabled
        // AND repeatTypeFixed are both selected.
        times.setEnabled(repeat && repeatTypeFixed.isSelected());

        // The Ok button is enabled if all the enabled text fields are correct.
        // This is a clever way of doing it, don't you think?
        boolean valid = true;
        for (JFormattedTextField tf: fields) {
            valid = valid && tf.isEditValid();
        }

        ok.setEnabled(valid);

    }

    public TimingWindowDialog(Frame owner) throws HeadlessException {
        super(owner, true);
        setTitle("Edit Timing Window");
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);





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
//                setBackground(new Color())
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
            add(labels[0] = new JLabel("forever"), gbc(1, 4, 2));
            add(repeatTypeFixed, gbc(0, 5, 1, new Insets(0, 20, 0, 0)));
            add(times, gbc(1, 5, 1));
            add(labels[1] = new JLabel(" times"), gbc(2, 5, 2));
            add(labels[2] = new JLabel("with a period of ", SwingConstants.RIGHT), gbc(1, 6, 2));
            add(period, gbc(3, 6, 1));
            add(labels[3] = new JLabel(" (hhh:mm:ss)."), gbc(4, 6, 1));
            add(new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)) {{
                setBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(10, 0, 0, 0)));
                add(ok);
                add(cancel);
            }}, gbc(0, 7, 6, new Insets(20, 0, 0, 0)));
        }});


        updateEnabledState();


        pack();
//        validate();

    }

    private static GridBagConstraints gbc(int x, int y, int xs) {
        return gbc(x, y, xs, new Insets(0, 0, 0, 0));
    }

    private static GridBagConstraints gbc(int x, int y, int xs, Insets insets) {
        return new GridBagConstraints(x, y + 1, xs, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0);
    }

    /**
     * Edit the passed TimingWindow object, returning a new one with the
     * edit results, or null if the edit was cancelled. This method blocks
     * until the user closes the dialog.
     * @param tw the original TimingWindow
     * @return a new TimingWindow, or null
     */
    public TimingWindow showEdit(TimingWindow tw) {
        setValue(tw);
        updateEnabledState();
        cancelled = true;
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return cancelled ? null : getValue();
    }

    private void setValue(TimingWindow initialValue) {

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

    private TimingWindow getValue() {
        long start = ((Date) window.getValue()).getTime();

        if (durationForever.isSelected()) {
            return new TimingWindow(start, TimingWindow.WINDOW_REMAINS_OPEN_FOREVER, TimingWindow.REPEAT_NEVER, 0);
        }

        long duration = (Long) this.duration.getValue();

        if (repeatEnabled.isSelected()) {
            long period = (Long) this.period.getValue();
            if (repeatTypeFixed.isSelected()) {
                int repeat = ((Number) times.getValue()).intValue();
                return new TimingWindow(start, duration, repeat, period);
            } else {
                return new TimingWindow(start, duration, TimingWindow.REPEAT_FOREVER, period);
            }
        } else {
            return new TimingWindow(start, duration, TimingWindow.REPEAT_NEVER, 0);
        }
    }
}






