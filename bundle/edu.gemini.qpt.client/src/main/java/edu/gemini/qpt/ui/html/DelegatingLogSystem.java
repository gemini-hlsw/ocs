package edu.gemini.qpt.ui.html;

import java.util.logging.Logger;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

/**
 * A Velocity logger that just delegates to JDK logging. Idiotic that I
 * had to write this.
 * @author rnorris
 */
public final class DelegatingLogSystem implements LogSystem {            

    private static final Logger LOGGER = Logger.getLogger(DelegatingLogSystem.class.getName());
    
    public void logVelocityMessage(int level, String msg) {
        
        // HACK: I dont care about this error.
        if (msg.contains("unable to find resource 'VM_global_library.vm' in any resource loader"))
            level = DEBUG_ID;
        
        switch (level) {
            case DEBUG_ID: LOGGER.fine(msg); break;
            case ERROR_ID: LOGGER.severe(msg); break;
            case INFO_ID: LOGGER.info(msg); break;
            case WARN_ID: LOGGER.warning(msg); break;
            default:
                LOGGER.info("Unknown level (" + level + "): " + msg);
        }
    }

    public void init(RuntimeServices rs) throws Exception {
        // NOP
    }

}

