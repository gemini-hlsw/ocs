package edu.gemini.ui.gface;

/**
 * Interface for an observer who is notified just before the selection is pushed. This gives
 * you the chance to change the model in the viewer, for example, in the case of master-detail
 * views. 
 * @author rnorris
 * @param <M> model type
 * @param <E> element type
 */
public interface GSelectionInterloper<M, E> extends GViewerPlugin<M, E> {

    /**
     * Called post-translation, before the selection task executes.
     * @param newSelection
     */
    void beforeSetSelection(GSelection<?> newSelection);

}
