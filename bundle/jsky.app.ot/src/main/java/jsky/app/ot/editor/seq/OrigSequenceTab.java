//
// $Id$
//

package jsky.app.ot.editor.seq;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obs.plannedtime.SetupTime;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.TimeAmountFormatter;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.seq.OrigSequenceTableModel.TitleColumns;
import jsky.app.ot.editor.seq.OrigSequenceTableModel.RowType;
import jsky.app.ot.util.OtColor;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import static jsky.app.ot.util.OtColor.*;

/**
 *
 */
final class OrigSequenceTab {
   // Factor for zooming timeline
    private static final double TIMELINE_ZOOM_FACTOR = 1.4;

    private static final class Attr {
        final Font  font;
        final Color bg;
        Attr(Font font, Color bg) {
            this.font = font;
            this.bg   = bg;
        }
    }

    private static final Font NORM = UIManager.getFont("Panel.font");
    private static final Font BOLD = NORM.deriveFont(Font.BOLD);
    private static final Map<RowType, Attr> ATTRS = new HashMap<RowType, Attr>() {{
        put(RowType.title,  new Attr(BOLD, LIGHT_ORANGE));
        put(RowType.system, new Attr(BOLD, VERY_LIGHT_GREY));
        put(RowType.data,   new Attr(NORM, VERY_LIGHT_GREY));
        put(RowType.empty,  new Attr(NORM, BG_GREY));
    }};

    private static class CellRenderer extends DefaultTableCellRenderer {

        String selectedDatasetLabel;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lab = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);

            OrigSequenceTableModel model = (OrigSequenceTableModel) table.getModel();

            RowType type = model.getType(row);
            Attr attr = ATTRS.get(type);
            lab.setFont(attr.font);

            Color bg = attr.bg;
            lab.setHorizontalAlignment(LEFT);

            // Show mapping errors.
            if (model.hasMappingError(row)) bg = LIGHT_SALMON;

            // Align the exposure time to the RIGHT.
            if (type == RowType.title) {
                switch (TitleColumns.values()[column]) {
                    case obsExposure:
                        lab.setHorizontalAlignment(RIGHT);
                        break;
                }
            }

            // If there is a selected dataset, draw rows that aren't associated
            // with it a bit darker (except for the empty spaces between
            // datasets).
            String curDatasetLabel = model.getDatasetLabel(row);
            if ((selectedDatasetLabel != null) && (curDatasetLabel != null) && !selectedDatasetLabel.equals(curDatasetLabel)) {
                bg = OtColor.makeSlightlyDarker(bg);
            }
            lab.setBackground(bg);

            return lab;
        }
    }

    // the GUI layout panel
    private IterFolderForm _w;
    private final EdIteratorFolder _parent;

    // cached current SP data
    private OtItemEditor _owner;

    private final OrigSequenceTableModel model = new OrigSequenceTableModel();

    OrigSequenceTab(IterFolderForm iff, EdIteratorFolder parent) {
        _w = iff;
        _parent = parent;

        final CellRenderer rend = new CellRenderer();
        _w.table.setModel(model);
        _w.table.setDefaultRenderer(String.class, rend);
        _w.table.getTableHeader().setVisible(false);
        _w.table.getTableHeader().setPreferredSize(new Dimension(-1, 0));

        _w.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _w.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int row = _w.table.getSelectedRow();
                if (row < 0) {
                    rend.selectedDatasetLabel = null;
                } else {
                    OrigSequenceTableModel model = (OrigSequenceTableModel) _w.table.getModel();
                    String lab = model.getDatasetLabel(row);
                    rend.selectedDatasetLabel = lab;
                    _w.timeline.selectDatasetNode(lab);
                    scrollTimeline();
                }
                _w.table.repaint();
            }

            private void scrollTimeline() {
                ObserveTimeLineNode node = _w.timeline.getSelectedNode();
                if (node == null) return;
                _w.timeline.scrollRectToVisible(node.getBounds());
            }
        });

        _w.timeline.addObserveTimeLineListener(new ObserveTimeLine.ObserveTimeLineListener() {
            @Override public void showSetup(ObserveTimeLine.ObserveTimeLineEvent event) {
                Rectangle zero = _w.table.getCellRect(0, 0, false);
                _w.table.getSelectionModel().clearSelection();
                _w.table.scrollRectToVisible(zero);
            }

            @Override public void showStep(ObserveTimeLine.ObserveTimeLineEvent event) {
                int step = event.node.step;
                Config c = event.node.plannedTime.sequence.getStep(step);
                String l = c.getItemValue(Keys.DATALABEL_KEY).toString();
                OrigSequenceTableModel.RowInterval ri = model.getRowInterval(l);

                _w.table.getSelectionModel().setSelectionInterval(ri.start, ri.start);

                // Show as much of the dataset info as possible by making the
                // last row visible and then the first row.
                Rectangle end   = _w.table.getCellRect(ri.end,   0, false);
                _w.table.scrollRectToVisible(end);
                Rectangle start = _w.table.getCellRect(ri.start, 0, false);
                _w.table.scrollRectToVisible(start);
            }
        });

        _w.printButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    _w.table.setTitle(_owner.getProgramDataObject().getTitle() + " Observing Sequence");
                    _w.table.showPrintDialog();
                } catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }
        });

        _w.exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    SequenceTabUtil.exportAsXML(_owner.getContextObservation(), _w);
                } catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }
        });
        _w.exportButton.setVisible(false);

        _w.zoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zoom(true);
            }
        });
        _w.zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zoom(false);
            }
        });

    }

    void init(OtItemEditor owner) {
        _owner = owner;
    }

    private void placeGaps() {
        for (int i=0; i < model.getRowCount(); ++i) {
            if (model.getType(i) == OrigSequenceTableModel.RowType.empty) {
                _w.table.setRowHeight(i, 25);
            }
        }
    }

    private void resize() {
        SequenceTabUtil.resizeTableColumns(_w.table, model);
        placeGaps();
    }

    private PlannedTime getPlannedTime() {
        PlannedTime pt = null;

        try {
            ISPObservation obs = _owner.getContextObservation();
            if (obs != null) pt = PlannedTimeCalculator.instance.calc(obs);
        } catch (Exception e) {
            DialogUtil.error(e);
        }

        return (pt == null) ? PlannedTime.apply(PlannedTime.Setup.apply(SetupTime.ZERO, ChargeClass.DEFAULT)) : pt;
    }

    /**
     * Display a text version of the observing sequence.
     */
    public void update() {
        _w.exportButton.setVisible(OTOptions.isStaff(_parent.getProgram().getProgramID()));

        PlannedTime pt = getPlannedTime();
        ConfigSequence cs = pt.sequence;
        model.setSequence(cs);
        resize();

        _w.timeline.update(pt);
        _w.totalTimeLabel.setText(TimeAmountFormatter.getHMSFormat(pt.totalTime()));
        _w.repaint();
    }

    protected void zoom(boolean zoomIn) {
        Dimension d = _w.timeline.getSize();
        JViewport vp = _w.timeLineScrollPane.getViewport();
        Rectangle r = vp.getViewRect();
        double f = TIMELINE_ZOOM_FACTOR;

        if (zoomIn) {
            int w = d.width;
            d.width *= f;
            vp.setViewPosition(new Point(r.x + (d.width - w) / 2, r.y));
        } else {
            int w = d.width;
            d.width /= f;
            vp.setViewPosition(new Point(Math.max(r.x - (w - d.width) / 2, 0), r.y));
        }

        _w.timeline.setPreferredSize(d);
        _w.timeline.revalidate();
        _w.timeLineScrollPane.repaint();
    }
}
