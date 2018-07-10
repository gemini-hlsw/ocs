package edu.gemini.qpt.ui.view.lchWindow;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

import javax.swing.JLabel;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LchWindowDecorator implements GSubElementDecorator<Schedule, LchWindow, LchWindowAttribute> {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private Schedule schedule;

    public void decorate(JLabel label, LchWindow element, LchWindowAttribute subElement, Object value) {
        if (schedule == null) return;

        TimePreference tp = TimePreference.BOX.get();
        switch (tp) {
            case LOCAL:
                TIME_FORMAT.setTimeZone(schedule.getSite().timezone());
                break;
            case SIDEREAL:
            case UNIVERSAL:
                TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
                break;
        }

        LchWindowController.Element e = (LchWindowController.Element) value;
        Date time = new Date(e.time);
        switch (subElement) {
            case Start:
            case End:
                if (tp == TimePreference.SIDEREAL) {
                    ImprovedSkyCalc calc = new ImprovedSkyCalc(schedule.getSite());
                    label.setText(TIME_FORMAT.format(calc.getLst(time)));
                } else {
                    label.setText(TIME_FORMAT.format(time));
                }
                break;
            case Length:
                label.setText(TimeUtils.msToHHMMSS(e.time)); // a length of time in "HH:mm:ss"
                break;
            case Type:
                label.setText(e.targetType);
                break;
            case Name:
                label.setText(e.targetName);
                break;
        }
        // Display in bold text if current alloc overlaps
        label.setFont(label.getFont().deriveFont(e.overlap ? Font.BOLD : Font.PLAIN));
    }

    public void modelChanged(GViewer<Schedule, LchWindow> viewer, Schedule oldModel, Schedule newModel) {
        schedule = newModel;
    }
}
