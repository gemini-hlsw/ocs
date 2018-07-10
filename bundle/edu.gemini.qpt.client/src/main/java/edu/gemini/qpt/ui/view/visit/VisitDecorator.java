package edu.gemini.qpt.ui.view.visit;

import static edu.gemini.qpt.ui.util.SharedIcons.*;
import static edu.gemini.qpt.ui.view.visit.VisitAttribute.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.ui.util.ColorWheel;
import edu.gemini.qpt.ui.util.CompositeIcon;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.spModel.core.ProgramTypeEnum;

public class VisitDecorator implements GSubElementDecorator<Variant, Alloc, VisitAttribute>, PropertyChangeListener {

    private final DateFormat localDF = new SimpleDateFormat("HH:mm");
    private final DateFormat universalDF = new SimpleDateFormat("HH:mm");

    private ImprovedSkyCalc calc;
    private GViewer<Variant, Alloc> viewer;
    private JTable table;

    private static final Color SHADOW_COLOR =     new Color(0xCCCCCC);
    private final Border NORMAL, NEXT, MIDDLE, FIRST, LAST, SOLO; // initialized at bottom


    public VisitDecorator() {
        universalDF.setTimeZone(TimeZone.getTimeZone("UTC"));
        TimePreference.BOX.addPropertyChangeListener(this);
    }

    public void decorate(JLabel label, Alloc alloc, VisitAttribute subElement, Object value) {

        // Text
        if (subElement == Start) {
            switch (TimePreference.BOX.get()) {
            case LOCAL:     label.setText(localDF.format(value)); break;
            case UNIVERSAL:label.setText(universalDF.format(value)); break;
            case SIDEREAL:
                label.setText(universalDF.format(calc.getLst((Date) value)));
                break;

            }
        }

        // Alignment
        switch (subElement) {
        case Group:
        case Start:
        case BG:
        case Dur:     label.setHorizontalAlignment(SwingConstants.CENTER); break;
        default:    label.setHorizontalAlignment(SwingConstants.LEFT);
        }

        // Icon
        if (subElement == Observation) {
            Obs obs = (Obs) value;

            // First, get the base icon
            if (obs.getProg().isType(ProgramTypeEnum.ENG)) {
                label.setIcon(ICON_DAYENG);
            } else if (obs.getProg().isType(ProgramTypeEnum.CAL)) {
                label.setIcon(ICON_DAYCAL);
            } else {
                switch (obs.getObsClass()) {
                case ACQ:
                case ACQ_CAL:         label.setIcon(ICON_ACQ);         break;
                case DAY_CAL:
                case PARTNER_CAL:
                case PROG_CAL:         label.setIcon(ICON_CALIB);     break;
                case SCIENCE:         label.setIcon(ICON_SCIENCE);     break;

                }
            }

            // Now add decoration as necessary
            Severity sev = alloc.getSeverity();
            if (sev != null) {
                switch (sev) {
                case Error:     label.setIcon(new CompositeIcon(label.getIcon(), OVL_ERROR)); break;
                case Warning:     label.setIcon(new CompositeIcon(label.getIcon(), OVL_WARN));
                }
            }

        } else {
            label.setIcon(null);
        }

        // Border and background color
        final Border border;
        final Color background;
        if (subElement == VisitAttribute.Group) {
            int index = alloc.getGroupIndex();
            background = index == -1 ? table.getBackground() : ColorWheel.get(index);
            switch (alloc.getGrouping()) {
            case FIRST: border = FIRST; break;
            case MIDDLE: border = MIDDLE; break;
            case LAST: border = LAST; break;
            case SOLO: border = SOLO; break;
            case NONE:
            default:
                Alloc next = alloc.getNext();
                if (next != null && next.getGroupIndex() != index)
                    border = NEXT;
                else
                    border = NORMAL;
                break;
            }
        } else {
            if (!label.getBackground().equals(table.getSelectionBackground())) {
                background = table.getBackground();
            } else {
                background = table.getSelectionBackground();
            }
            border = NORMAL;
        }
        label.setBorder(border);
        label.setBackground(background);

    }

    public void modelChanged(GViewer<Variant, Alloc> viewer, Variant oldModel, Variant newModel) {
        this.viewer = viewer;
        this.table = (JTable) viewer.getControl();
        if (newModel != null) {
            localDF.setTimeZone(newModel.getSchedule().getSite().timezone());
            calc = new ImprovedSkyCalc(newModel.getSchedule().getSite());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        viewer.refresh();
    }

    {
        NORMAL = new CellBorder(){
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(table.getGridColor());
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1); // bottom
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);  // right
            }
        };

        NEXT = new CellBorder(){
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(table.getGridColor());
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);  // right
                g.setColor(SHADOW_COLOR);
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1); // bottom
            }
        };


        MIDDLE = new CellBorder() {
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(SHADOW_COLOR);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);  // right
            }
        };

        FIRST = new CellBorder() {
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(SHADOW_COLOR);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);  // right
            }
        };

        LAST = new CellBorder() {
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(SHADOW_COLOR);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);  // right
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);  // bottom
            }
        };

        SOLO = new CellBorder() {
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(SHADOW_COLOR);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);  // right
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);  // bottom
            }
        };

    }
}

abstract class CellBorder implements Border {

    private static final Insets INSETS = new Insets(1, 1, 2, 1);

    public Insets getBorderInsets(Component c) {
        return INSETS;
    }

    public boolean isBorderOpaque() {
        return false;
    }

}


