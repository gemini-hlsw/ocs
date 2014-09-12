package edu.gemini.obslog.config;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlModelException;
import edu.gemini.obslog.config.xml.OlXmlConfiguration;

import java.io.Serializable;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: OlDefaultConfigurationProducer.java,v 1.3 2005/03/17 06:04:33 gillies Exp $
//

public class OlDefaultConfigurationProducer implements OlConfigurationProducer, Serializable {
    public static final Logger LOG = Logger.getLogger(OlDefaultConfigurationProducer.class.getName());

    static private String _CONFIG_FILE = "/ObsLogConfig.xml";

    private OlConfiguration _configuration;

    /**
     * Default constructor uses default config file
     */
    public OlDefaultConfigurationProducer() {
    }

    public OlDefaultConfigurationProducer(String configFile) {
        if (configFile == null) throw new IllegalArgumentException();

        _CONFIG_FILE = configFile;
    }

    public synchronized OlConfiguration getModel() throws OlConfigException, OlModelException {
        if (_configuration == null) {
            LOG.fine("Reading the config file: " + _CONFIG_FILE);
            OlXmlConfiguration _cr = new OlXmlConfiguration(_CONFIG_FILE);
            _configuration = _cr.getModel();
        }
        return _configuration;
    }

}
