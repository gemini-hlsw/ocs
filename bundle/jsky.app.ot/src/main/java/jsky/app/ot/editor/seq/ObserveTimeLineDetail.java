package jsky.app.ot.editor.seq;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

import static jsky.app.ot.util.OtColor.VERY_LIGHT_GREY;

/**
 *
 */
final class ObserveTimeLineDetail extends JPanel implements ObserveTimeLine.ObserveTimeLineListener {

    private static class DetailTableModel extends AbstractTableModel {
        private Config prev;
        private Config cur;
        private ItemKey[] keys = new ItemKey[0];

        void update(PlannedTime plannedTime, int step) {
            Config prev = null;
            if (step >= 1) {
                prev = plannedTime.sequence.getStep(step-1);
            }
            this.prev = prev;
            this.cur = plannedTime.sequence.getCompactView()[step];

            // Filter out the crap here.
            keys = cur.getKeys();
            fireTableDataChanged();
        }

        private enum Col {
            item("Item") {
                public String extractValue(ItemKey key, Config cur, Config prev) {
                    return key.getPath();
                }
            },
            value("Value") {
                private String val(ItemKey key, Config c) {
                    Object res = c.getItemValue(key);
                    return res == null ? "" : res.toString();
                }
                public String extractValue(ItemKey key, Config cur, Config prev) {
                    String c = val(key, cur);
                    return (prev == null) ? c :
                        String.format("%s -> %s", val(key, prev), c);
                }
            },
            ;

            private final String display;

            Col(String display) { this.display = display; }
            public String display() { return this.display; }
            public abstract String extractValue(ItemKey key, Config cur, Config prev);
        }

        public int getRowCount() { return keys.length; }
        public String getColumnName(int col) { return Col.values()[col].display(); }
        public int getColumnCount() { return Col.values().length; }
        public Object getValueAt(int row, int col) {
            return Col.values()[col].extractValue(keys[row], cur, prev);
        }
    }

    private final DetailTableModel tableModel = new DetailTableModel();

    ObserveTimeLineDetail() {
        add(createConfigTablePanel(tableModel));
    }

    private JPanel createConfigTablePanel(DetailTableModel tm) {
        JPanel pan = new JPanel(new BorderLayout());

        JTable configTable = new JTable(tm) {{
            setAutoResizeMode(AUTO_RESIZE_OFF);
            setBackground(VERY_LIGHT_GREY);
            getTableHeader().setReorderingAllowed(false);
            setRowSelectionAllowed(false);
            setColumnSelectionAllowed(false);
            setFocusable(false);
        }};
        configTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        configTable.setBackground(VERY_LIGHT_GREY);
        configTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane sp = new JScrollPane();
        sp.setViewportView(configTable);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        pan.add(sp, BorderLayout.CENTER);

        return pan;
    }

    @Override public void showSetup(ObserveTimeLine.ObserveTimeLineEvent event) {
    }

    @Override public void showStep(ObserveTimeLine.ObserveTimeLineEvent event) {
        tableModel.update(event.node.plannedTime, event.node.step);
    }
}
