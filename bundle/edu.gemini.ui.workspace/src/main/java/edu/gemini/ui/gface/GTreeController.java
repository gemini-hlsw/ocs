package edu.gemini.ui.gface;

import java.util.Collection;

public interface GTreeController<M, E> extends GViewerPlugin<M, E> {

    E getRoot();

    Collection<E> getChildren(E parent);
}
