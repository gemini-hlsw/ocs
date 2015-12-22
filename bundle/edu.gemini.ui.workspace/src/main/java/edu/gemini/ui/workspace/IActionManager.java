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

     */
    void addContainer(Relation rel, String path, String id, String caption);

    /**
     * Add an action relative to the specified path.
     */
    void addAction(Relation rel, String path, String id, Action action);

    /**
     * Add a separator relative to the specified path.
     */
    void addSeparator(Relation rel, String path);

}
