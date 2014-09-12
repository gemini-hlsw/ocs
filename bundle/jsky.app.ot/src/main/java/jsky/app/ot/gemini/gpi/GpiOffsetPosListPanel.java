package jsky.app.ot.gemini.gpi;

import jsky.app.ot.gemini.editor.offset.AbstractOffsetPosListPanel;

public final class GpiOffsetPosListPanel extends AbstractOffsetPosListPanel {
	public GpiOffsetPosListPanel() {
        super(new GpiOffsetPosPanel());
    }

    public GpiOffsetPosPanel getGpiOffsetPosEditorUI() {
        return (GpiOffsetPosPanel) getOffsetPosEditorUI();
    }
}