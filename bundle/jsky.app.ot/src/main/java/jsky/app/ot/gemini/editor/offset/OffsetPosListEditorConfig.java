package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.target.offset.OffsetPosBase;

/**
 * Contains all the objects needed by {@link AbstractOffsetPosListEditor}.
 */
public final class OffsetPosListEditorConfig<P extends OffsetPosBase> {
    private final AbstractOffsetPosListPanel pan;
    private final AbstractOffsetPosTableEditor<P> tableEditor;
    private final AbstractOffsetPosEditor<P> posEditor;

    public OffsetPosListEditorConfig(AbstractOffsetPosListPanel pan, AbstractOffsetPosTableEditor<P> tableEditor, AbstractOffsetPosEditor<P> posEditor) {
        this.pan         = pan;
        this.tableEditor = tableEditor;
        this.posEditor   = posEditor;
    }

    public AbstractOffsetPosListPanel getPan() {
        return pan;
    }

    public AbstractOffsetPosTableEditor<P> getTableEditor() {
        return tableEditor;
    }

    public AbstractOffsetPosEditor<P> getPosEditor() {
        return posEditor;
    }
}
