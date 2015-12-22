package edu.gemini.ui.gface;

/**
 * Controller that maps model M to element type E.
 * @author rnorris
 * @param <M> model type
 * @param <E> element type
 */
public interface GViewerPlugin<M, E> {
    void modelChanged(GViewer<M, E> viewer, M oldModel, M newModel);
}
