package jsky.app.ot.gemini.nici;

import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosTableEditor;
import jsky.app.ot.gemini.editor.offset.OffsetPosTablePanel;
import edu.gemini.spModel.gemini.nici.NiciOffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;

import java.util.List;

/**
 * Table editor for {@link edu.gemini.spModel.gemini.nici.NiciOffsetPos NICI
 * offset positions}.
 */
public class NiciOffsetPosTableEditor extends AbstractOffsetPosTableEditor<NiciOffsetPos> {
    protected NiciOffsetPosTableEditor(OffsetPosTablePanel pan, NiciOffsetPosTableModel tableModel) {
        super(pan, tableModel);
    }

    protected void addPosition() {
        OffsetPosList<NiciOffsetPos> opl = getPositionList();
        List<NiciOffsetPos> selList = getAllSelectedPos();

        NiciOffsetPos pos;
        if (selList.size() == 0) {
            pos = opl.addPosition(0);
        } else {
            NiciOffsetPos lastSelPos =  selList.get(selList.size()-1);
            int i = opl.getPositionIndex(lastSelPos);
            pos = opl.addPosition(i + 1);
            pos.setFpmwTacking(lastSelPos.isFpmwTracking(), getPort());
        }
        select(pos);
    }
}
