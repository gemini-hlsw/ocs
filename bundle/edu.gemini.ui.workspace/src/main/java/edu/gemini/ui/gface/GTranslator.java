package edu.gemini.ui.gface;

import java.util.Set;

/**
 * Interface for a type translator from Object to E. This facility is used in the
 * selection subsystem to allow a viewer to translate an arbitrary untyped selection 
 * into a typed selection. For instance, a directory view might translate a file
 * into its parent directory, or vice-versa.
 * @author rnorris
 * @param <M>
 * @param <E>
 */
public interface GTranslator<M, E> extends GViewerPlugin<M, E> {

	/**
	 * Returns the translation of o into the viewer's domain, or the empty set if
	 * no translation is possible.
	 * @param o
	 * @return a Set<E>, possibly empty
	 */
	Set<E> translate(Object o); // RCN: Set<E>?
	
}
