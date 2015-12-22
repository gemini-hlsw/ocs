package edu.gemini.ui.gface;

/**
 * Controller that maps model M to a list of E
 * @author rnorris
 * @param <M> model type
 * @param <E> element type
 */
public interface GListController<M, E> extends GViewerPlugin<M, E> {
    int getElementCount();

    E getElementAt(int row);
}



