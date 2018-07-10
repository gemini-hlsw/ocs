package edu.gemini.qpt.core.listeners;

/**
 * Interface for a listener that wishes to watch model objects of type T.
 * @author rnorris
 */
public interface ModelListener<T> {

    void subscribe(T o);
    
    void unsubscribe(T o);
    
}

