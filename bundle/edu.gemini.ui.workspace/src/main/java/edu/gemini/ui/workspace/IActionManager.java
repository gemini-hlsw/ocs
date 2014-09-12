package edu.gemini.ui.workspace;

import javax.swing.Action;

public interface IActionManager {

	enum Relation {
		FirstChildOf,
		LastChildOf,
		PreviousSiblingOf,
		NextSiblingOf,
	}
	
	/**
	 * Add a menu/toolbar relative to the specified path, or "" for the root.
	 * @param rel
	 * @param path
	 * @param id
	 * @param caption
	 */
	void addContainer(Relation rel, String path, String id, String caption);
	
	/**
	 * Add an action relative to the specified path.
	 * @param rel
	 * @param path
	 * @param id
	 * @param action
	 */
	void addAction(Relation rel, String path, String id, Action action);
	
//	/**
//	 * Add an action place-holder relative to the specified path.
//	 * @param rel
//	 * @param path
//	 * @param id
//	 * @param caption
//	 */
//	void addAction(Relation rel, String path, String id, String caption);
	
	/**
	 * Add a separator relative to the specified path.
	 * @param rel
	 * @param path
	 */
	void addSeparator(Relation rel, String path);

}
