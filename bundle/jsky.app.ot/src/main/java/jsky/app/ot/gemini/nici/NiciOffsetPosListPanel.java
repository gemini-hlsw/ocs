package jsky.app.ot.gemini.nici;

import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosListPanel;

public final class NiciOffsetPosListPanel extends AbstractOffsetPosListPanel {
	public NiciOffsetPosListPanel() {
        super(new NiciOffsetPosPanel());
    }

    public NiciOffsetPosPanel getNiciOffsetPosEditorUI() {
        return (NiciOffsetPosPanel) getOffsetPosEditorUI();
    }
}
