package edu.gemini.ui.gface;

/**
 * Controller that maps a model M to a table of E.
 * @author rnorris
 * @param <M> model type
 * @param <E> element type
 * @param <S> subElement identifier type (generally an enum)
 */
public interface GTableController<M, E, S> extends GListController<M, E> {

    Object getSubElement(E element, S subElement);

}
