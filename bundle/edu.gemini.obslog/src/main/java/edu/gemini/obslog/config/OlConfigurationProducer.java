package edu.gemini.obslog.config;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlModelException;

//
// Gemini Observatory/AURA
// $Id: OlConfigurationProducer.java,v 1.1.1.1 2004/11/16 02:56:24 gillies Exp $
//

public interface OlConfigurationProducer {

    public OlConfiguration getModel() throws OlConfigException, OlModelException;

}

