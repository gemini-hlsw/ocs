package edu.gemini.obslog.actions;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.database.pot.OlPOTRemotePersistenceManager;
import edu.gemini.obslog.config.OlConfigurationProducer;
import edu.gemini.obslog.config.OlDefaultConfigurationProducer;

/**
 * Gemini Observatory/AURA
 * $Id$
 */
public class GuiceModule extends AbstractModule {

    protected void configure() {
        System.out.println("Configure got called!");
        bind(OlPersistenceManager.class).to(OlPOTRemotePersistenceManager.class).in(Scopes.SINGLETON);

        bind(OlConfigurationProducer.class).to(OlDefaultConfigurationProducer.class).in(Scopes.SINGLETON);
    }
}
