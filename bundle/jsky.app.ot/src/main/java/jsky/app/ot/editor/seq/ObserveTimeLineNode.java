// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ObserveTimeLineNode.java 38416 2011-11-07 14:19:47Z swalker $
//
package jsky.app.ot.editor.seq;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.*;
import jsky.app.ot.util.OtColor;
import jsky.science.Time;
import jsky.timeline.DefaultTimeLineNode;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Map;


/**
 * A TimeLine node for use with the ObserveTimeLine class.
 * This class redefines getDescription(Point) to return information about the
 * observe sequence.
 *
 * @author Allan Brighton (but then hacked by Shane)
 */
public class ObserveTimeLineNode extends DefaultTimeLineNode {

    private static final Color selected = OtColor.LIGHT_ORANGE;
    private static final Color unselected = OtColor.makeSlightlyDarker(OtColor.makeSlightlyDarker(selected));

    /** object containing a description of this node */
    public final PlannedTime plannedTime;
    public final int step;

    /**
     * Initialize an observing node with the start time, duration, and an
     * ISysConfig object containing a description of the node.
     */
    public ObserveTimeLineNode(double startTime, PlannedTime pt, int step) {
        super(new Time(startTime, Time.SECOND),
              new Time(startTime + getDuration(pt, step)/1000.0, Time.SECOND),
              getNodeLabel(pt, step));

        this.plannedTime = pt;
        this.step        = step;

        // Customize the display a bit.
        fHandleWidth     = 2.0f;
        fHandleHeight    = 8.0f;

        if (step < 0) {
            setSelectedColor(Color.gray);
            setUnselectedColor(OtColor.makeSlightlyDarker(Color.gray));
        } else {
            setSelectedColor(selected);
            setUnselectedColor(unselected);
        }
    }

    /**
     * Return the label to display for the given sysConfig.
     * In this case we want to display the type of the observe
     * nodes instead of just "observe".
     */
    public static String getNodeLabel(PlannedTime pt, int step) {
        return (step < 0) ? "Setup" : SequenceTabUtil.shortDatasetLabel(pt.sequence.getStep(step));
                //pt.sequence.getStep(step).getItemValue(Keys.DATALABEL_KEY).toString();
    }

    @Override public String shortenTimeLineNodeName(String start) {
        if (start == null) return null;
        int i = start.indexOf("-");
        if ((i < 0) || ((i+1) == start.length())) {
            return (start.startsWith("0")) ? start.replaceFirst("0+", "") : null;
        }
        return start.substring(i+1);
    }

    public static long getDuration(PlannedTime pt, int step) {
        return (step < 0) ? pt.setup.time.toDuration().toMillis() : pt.steps.get(step).totalTime();
    }

    @Override public String getDescription(Point pt) {
        StringBuilder buf = new StringBuilder("<html><body>");
        if (step < 0) {
            buf.append("<b>Setup ").append(formatSec(plannedTime.setup.time.toDuration().toMillis())).append(" sec</b>");
        } else {
            buf.append("<p align=\"center\"><b>").append(SequenceTabUtil.shortDatasetLabel(plannedTime.sequence.getStep(step))).append("</b></p>");

            buf.append("<table><tr><th colspan=\"2\">Event</th><th colspan=\"2\">Sec</th></tr>");
            Step s = plannedTime.steps.get(step);
            Map<Category, ImList<CategorizedTime>> m = s.times.groupTimes();
            for (Category c : Category.values()) {
                ImList<CategorizedTime> cts = m.get(c);
                if (cts == null) continue;
                CategorizedTime ct = cts.max(Comparator.naturalOrder());
                buf.append("<tr>");
                buf.append("<td colspan=\"3\">").append(c.display).append("</td>");

                String secStr = formatSec(ct.time);
                buf.append("<td align=\"right\"> ").append(secStr).append("</td>");
                buf.append("</tr>");

                // Only Config changes have separate actions running in parallel.
                if (ct.category == Category.CONFIG_CHANGE) {
                    buf.append("<tr>");
                    buf.append("<td></td>");
                    buf.append("<td>").append(formatCategory(ct)).append("</td>");

                    buf.append("<td align=\"right\"> ").append(secStr).append("</td>");
                    buf.append("<td></td>");
                    buf.append("</tr>");

                    ImList<CategorizedTime> lcts = m.get(c);
                    if (lcts == null) continue;
                    ImList<CategorizedTime> lctst = lcts.sort(Comparator.reverseOrder()).tail();
                    for (CategorizedTime lct : lctst) {
                        if (lct == null) continue;
                        buf.append("<tr>");
                        buf.append("<td></td>");
                        buf.append("<td>").append(formatCategory(lct)).append("</td>");

                        String secLosStr = formatSec(lct.time);
                        buf.append("<td align=\"right\"> ").append(secLosStr).append("</td>");
                        buf.append("<td></td>");
                        buf.append("</tr>");
                    }
                }
            }
            buf.append("<tr><td colspan=\"3\"><strong>Total</strong></td><td align=\"right\"><strong>").append(formatSec(s.totalTime())).append("</strong></td></tr>");
            buf.append("</table>");
        }
        buf.append("</body></html>");
        return buf.toString();
    }

    private static String formatCategory(CategorizedTime ct) {
        return (ct.detail == null) ? ct.category.display : ct.detail;
    }

    private static String formatSec(long time) {
        return String.format("%.1f", time/1000.0);
    }

    public void handleMouseMoveEvent(MouseEvent evt) {
        // do nothing
    }

    public synchronized void handleMouseEvent(MouseEvent evt) {
        // do nothing
    }
}
