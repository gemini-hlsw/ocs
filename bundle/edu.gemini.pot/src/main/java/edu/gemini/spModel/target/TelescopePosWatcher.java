package edu.gemini.spModel.target;

/**
 * An interface supported by clients of TelescopePos who want to
 * be notified when the positions changes in some way.
 */
@FunctionalInterface
public interface TelescopePosWatcher {

    void telescopePosUpdate(WatchablePos tp);

}



