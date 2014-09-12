package edu.gemini.ui.gface;

public interface GFilter<M, E> extends GViewerPlugin<M, E> {

	boolean accept(E element);
	
}
