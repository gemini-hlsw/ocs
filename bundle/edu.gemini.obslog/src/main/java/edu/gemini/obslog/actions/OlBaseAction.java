package edu.gemini.obslog.actions;

import com.opensymphony.xwork2.ActionSupport;
import com.google.inject.Inject;
import edu.gemini.obslog.config.OlConfigException;
import edu.gemini.obslog.config.OlConfigurationProducer;
import edu.gemini.obslog.config.OlConfigurationProducerAware;
import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlModelException;
import edu.gemini.obslog.database.OlPersistenceAware;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.util.security.principal.StaffPrincipal;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: OlBaseAction.java,v 1.5 2005/06/15 05:43:16 gillies Exp $
//

/**
 * This class is the base class for all Actions.  It primarily provides the get/set methods for the components set
 * from the IOC part of webworks.  The <tt>PersistenceManager</tt> and <tt>ConfigurationManager</tt> are set by IOC from
 * application "singletons" maintained by the framework.
 */
public abstract class OlBaseAction extends ActionSupport implements OlConfigurationProducerAware, OlPersistenceAware {
    private static final Logger LOG = Logger.getLogger(OlBaseAction.class.getName());

    private OlConfigurationProducer _configurationProducer;
    private OlPersistenceManager _persistenceManager;

    // Our actions all run as the superuser :-\
    protected Set<Principal> _user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    /**
     * Set the <code>{@link OlConfigurationProducer}</code> that provides the observing log configuration
     *
     * @param configurationProducer an <code>Ol
     */
    @Inject
    public void setObsLogConfigurationProducer(OlConfigurationProducer configurationProducer) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting Config Producer");
        }
        _configurationProducer = configurationProducer;
    }

    public void setTest(OlPersistenceManager pm) {
        _persistenceManager = pm;
    }
    /**
     * @param persistenceManager
     */
    @Inject
    public void setPersistenceManager(OlPersistenceManager persistenceManager) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting Persistence Manager");
        }
        _persistenceManager = persistenceManager;
    }

    protected OlPersistenceManager getPersistenceManager() {
        return _persistenceManager;
    }

    protected OlConfigurationProducer getConfigurationManager() {
        return _configurationProducer;
    }

    protected OlConfiguration getObsLogConfiguration() throws OlLogException {
        OlConfigurationProducer producer = getConfigurationManager();
        if (producer == null) throw new NullPointerException("No observing log configuration available.");

        OlConfiguration configuration = null;
        try {
            configuration = producer.getModel();
        } catch (OlConfigException ex) {
            throw new OlLogException("A configuration error occured while reading the config file.");
        } catch (OlModelException ex) {
            throw new OlLogException("A model error occured while reading the config file.");
        }
        return configuration;
    }
}

