package jsky.app.ot.progadmin;

import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.timeacct.TimeAcctAllocation;
import edu.gemini.spModel.timeacct.TimeAcctCategory;
import jsky.coords.HMS;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class TimeAcctEditor implements ProgramTypeListener {
    // Used to format values as strings
    private final static NumberFormat nf = NumberFormat.getInstance(Locale.US);
    static {
        nf.setMinimumIntegerDigits(1);
        nf.setMaximumFractionDigits(1);
    }

    private final TimeAcctUI ui;

    public TimeAcctEditor(final TimeAcctUI ui, final ProgramTypeModel pkm) {
        this.ui = ui;

        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JTextField field = ui.getHoursField(cat);
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(final DocumentEvent event) {
                    update();
                }

                @Override public void removeUpdate(final DocumentEvent event) {
                    update();
                }

                @Override public void changedUpdate(final DocumentEvent event) {
                    update();
                }

                private void update() {
                    final TimeAcctAllocation alloc = getAllocation();
                    showTotalTime(alloc);
                    showPercentTime(alloc);
                }
            });
        }

        final boolean enable = enableMinimumTime(pkm.getProgramType());
        if (!enable) ui.getMinimumTimeField().setText("");
        ui.getMinimumTimeField().setEnabled(enable);

        pkm.addProgramTypeListener(this);
    }

    public TimeAcctAllocation getAllocation() {

        final Map<TimeAcctCategory, Double> hoursMap = new HashMap<>();
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JTextField field = ui.getHoursField(cat);
            final String hoursStr = field.getText();
            try {
                hoursMap.put(cat, Double.parseDouble(hoursStr));
            } catch (Exception ex) {
                // ignore
            }
        }
        return new TimeAcctAllocation(hoursMap);
    }

    private void showTotalTime(final TimeAcctAllocation alloc) {
        // Show the total time awarded without trailing 0s
        String hoursStr = String.format("%.2f", alloc.getTotalTime());
        if (hoursStr.endsWith(".00")) {
            hoursStr = hoursStr.substring(0, hoursStr.length() - 3);
        } else if (hoursStr.endsWith("0")) {
            hoursStr = hoursStr.substring(0, hoursStr.length() - 1);
        }
        ui.getTimeAwardedLabel().setText(hoursStr);
    }

    private TimeValue getMinimumTime() {
        final String text = ui.getMinimumTimeField().getText();
        if ((text == null) || "".equals(text.trim())) return TimeValue.ZERO_HOURS;

        double hours = 0;
        try {
            hours = Double.parseDouble(text);
            if (hours < 0) hours = 0;
        } catch (NumberFormatException nfex) {
            // If the value can't be parsed as a double, then try the
            // HH:MM:SS format
            try {
                hours = (new HMS(text).getVal());
            } catch (Exception ex) {
                // give up trying to parse the value
            }
        }

        return new TimeValue(hours, TimeValue.Units.hours);
    }

    private void showPercentTime(final TimeAcctAllocation alloc) {
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JLabel lab = ui.getPercentLabel(cat);
            final double percent = alloc.getPercentage(cat);
            lab.setText(String.format("%.0f%%", percent));
        }
    }

    public void setModel(final TimeAcctModel model) {
        TimeValue minTime = model.getMinimumTime();

        if ((minTime == null) || (minTime.getMilliseconds() == 0)) {
            ui.getMinimumTimeField().setText("");
        } else {
            minTime = minTime.convertTo(TimeValue.Units.hours);
            ui.getMinimumTimeField().setText(nf.format(minTime.getTimeAmount())); // REL-434
        }

        final TimeAcctAllocation alloc = model.getAllocation();
        showTotalTime(alloc);

        // Show the time for each category.
        for (final TimeAcctCategory cat : TimeAcctCategory.values()) {
            final JTextField field = ui.getHoursField(cat);
            final double hours = alloc.getHours(cat);
            field.setText(nf.format(hours)); // REL-434
        }

        // Show the percentage for each category.
        showPercentTime(alloc);
    }

    public TimeAcctModel getModel() {
        return new TimeAcctModel(getAllocation(), getMinimumTime());
    }

    public void programTypeChanged(final ProgramTypeEvent event) {
        final JTextField minTimeField = ui.getMinimumTimeField();

        final boolean enable = enableMinimumTime(event.getNewType());
        final Color bg;
        if (enable) {
            bg = Color.white;
        } else {
            minTimeField.setText("");
            bg = new Color(225, 225, 225);
        }
        minTimeField.setEnabled(enable);
        minTimeField.setBackground(bg);
    }

    private static boolean enableMinimumTime(final ProgramTypeInfo pk) {
        if (pk.getMode() != SPProgram.ProgramMode.QUEUE) return false;
        return pk.getQueueBand() == 3;
    }
}
