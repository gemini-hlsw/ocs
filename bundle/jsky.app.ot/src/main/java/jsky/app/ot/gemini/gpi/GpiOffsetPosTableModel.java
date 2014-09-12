package jsky.app.ot.gemini.gpi;

import edu.gemini.spModel.gemini.gpi.GpiOffsetPos;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosTableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GpiOffsetPosTableModel extends AbstractOffsetPosTableModel<GpiOffsetPos> {

    private static final List<Column<GpiOffsetPos>> FIXED_COLUMNS;

    static {
        List<Column<GpiOffsetPos>> lst = new ArrayList<Column<GpiOffsetPos>>(3);
        lst.add(new PColumn<GpiOffsetPos>(){
            public String getName() { return "X"; }
        });
        lst.add(new QColumn<GpiOffsetPos>(){
            public String getName() { return "Y"; }
        });
        lst.add(new DefaultGuideColumn<GpiOffsetPos>());
        FIXED_COLUMNS = Collections.unmodifiableList(lst);
    }

    protected GpiOffsetPosTableModel() {
        super(FIXED_COLUMNS);
    }
}
