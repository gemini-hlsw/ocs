package jsky.app.ot.gemini.nici;

import edu.gemini.spModel.gemini.nici.NiciOffsetPos;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosTableModel;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class NiciOffsetPosTableModel extends AbstractOffsetPosTableModel<NiciOffsetPos> {
    public static final String PQ_FORMAT = "%.4f";

    private static final Column<NiciOffsetPos> TRACKING_COLUMN = new Column<NiciOffsetPos>() {
        public String getName() {
            return "FPMW";
        }

        public Object getValue(NiciOffsetPos pos, int row) {
            return (pos.isFpmwTracking()) ? "Follow" : "Freeze";
        }

        public Class getColumnClass() {
            return String.class;
        }

        public GuideProbeState getGuideProbeState() {
            return GuideProbeState.notApplicable;
        }
    };

    private static final Column<NiciOffsetPos> D_COLUMN = new Column<NiciOffsetPos>() {
        public String getName() {
            return "d";
        }

        public Object getValue(NiciOffsetPos pos, int row) {
            if (pos.isFpmwTracking()) {
                return Double.toString(pos.getOffsetDistance());
            }
            return "";
        }

        public Class getColumnClass() {
            return String.class;
        }

        public GuideProbeState getGuideProbeState() {
            return GuideProbeState.notApplicable;
        }
    };

    private static final List<Column<NiciOffsetPos>> FIXED_COLUMNS;

    static {
        List<Column<NiciOffsetPos>> lst = new ArrayList<Column<NiciOffsetPos>>(5);
        lst.add(TRACKING_COLUMN);
        lst.add(D_COLUMN);
        lst.add(new PColumn<NiciOffsetPos>());
        lst.add(new QColumn<NiciOffsetPos>());
        lst.add(new DefaultGuideColumn<NiciOffsetPos>());
        FIXED_COLUMNS = Collections.unmodifiableList(lst);
    }

    protected NiciOffsetPosTableModel() {
        super(FIXED_COLUMNS);
    }
}
