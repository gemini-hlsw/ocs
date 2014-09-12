package edu.gemini.spdb.reports.impl.www;

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
		
		// HACK: I don't care about this error.
		if (msg.contains("unable to find resource 'VM_global_library.vm' in any resource loader"))
			level = DEBUG_ID;
		
		switch (level) {
            case ERROR_ID: LOGGER.severe(msg); break;
            case WARN_ID: LOGGER.warning(msg); break;
			case INFO_ID: LOGGER.fine(msg); break;
            case DEBUG_ID: LOGGER.finer(msg); break;
			default:
				LOGGER.info("Unknown level (" + level + "): " + msg);
		}
	}

	public void init(RuntimeServices rs) throws Exception {
		// NOP
	}

}

