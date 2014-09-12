package edu.gemini.obslog.database;

//
// Gemini Observatory/AURA
// $Id: OlPersistenceAware.java,v 1.1.1.1 2004/11/16 02:56:25 gillies Exp $
//

public interface OlPersistenceAware {
    public void setPersistenceManager(OlPersistenceManager persistenceManager);
}
