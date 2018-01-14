package edu.gemini.qpt.ui.view.lchWindow;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.shared.util.DateTimeFormatters;
import edu.gemini.shared.util.DateTimeUtils;
import edu.gemini.shared.util.UTCDateTimeFormatters;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

import javax.swing.JLabel;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class LchWindowDecorator implements GSubElementDecorator<Schedule, LchWindow, LchWindowAttribute> {

    private Schedule schedule;

    public void decorate(JLabel label, LchWindow element, LchWindowAttribute subElement, Object value) {
        if (schedule == null) return;

        TimePreference tp = TimePreference.BOX.get();
        final DateTimeFormatter df;
        switch (tp) {
            case LOCAL:
                df = DateTimeFormatters.apply(schedule.getSite().timezone()).HHMMSS();
                break;
            default:
                df = UTCDateTimeFormatters.HHMMSS();
                break;
        }

        LchWindowController.Element e = (LchWindowController.Element) value;
        Date time = new Date(e.time);
        switch (subElement) {
            case Start:
            case End:
                if (tp == TimePreference.SIDEREAL) {
                    ImprovedSkyCalc calc = new ImprovedSkyCalc(schedule.getSite());
                    label.setText(df.format(calc.getLst(time).toInstant()));
                } else {
                    label.setText(df.format(time.toInstant()));
                }
                break;
            case Length:
                label.setText(DateTimeUtils.msToHMMSS(e.time)); // a length of time in "HH:mm:ss"
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
