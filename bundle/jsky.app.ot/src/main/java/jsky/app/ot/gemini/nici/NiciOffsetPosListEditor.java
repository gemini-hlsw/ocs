package jsky.app.ot.gemini.nici;

import edu.gemini.spModel.gemini.nici.NiciOffsetPos;
import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosListEditor;
import jsky.app.ot.gemini.editor.offset.OffsetPosListEditorConfig;

/**
 * This is the editor for Offset Iterator component.  It allows a list of
 * offset positions to be entered and ordered.
 *
 * @see edu.gemini.spModel.gemini.nici.NiciOffsetPos
 */
public final class NiciOffsetPosListEditor extends AbstractOffsetPosListEditor<NiciOffsetPos> {

    /**
     * The constructor initializes the user interface.
     */
    public NiciOffsetPosListEditor() {
        NiciOffsetPosListPanel pan = new NiciOffsetPosListPanel();
        NiciOffsetPosTableModel tableModel = new NiciOffsetPosTableModel();
        NiciOffsetPosTableEditor tableEditor = new NiciOffsetPosTableEditor(pan.getOffsetPosTablePanel(), tableModel);
        NiciOffsetPosEditor posEditor = new NiciOffsetPosEditor(pan.getNiciOffsetPosEditorUI());

        OffsetPosListEditorConfig<NiciOffsetPos> config;
        config = new OffsetPosListEditorConfig<NiciOffsetPos>(pan, tableEditor, posEditor);
        init(config);
    }

}
