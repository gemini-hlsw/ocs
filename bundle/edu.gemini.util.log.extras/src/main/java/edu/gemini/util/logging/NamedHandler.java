package edu.gemini.util.logging;

import java.util.logging.Handler;
import java.util.logging.LogManager;

public abstract class NamedHandler extends Handler {

	private final String name;
	
	/**
	 * Constructs a named handler. The name may be "".
	 * @param name
	 */
	protected NamedHandler(String name) {
		if (name == null)
			name = "";
		this.name = name;
	}
	
	/**
	 * Retrieves the specified LogManager property, prefixed by this handler's 
	 * classname. If the logger is named, the named instance will be consulted
	 * first. For example, if handler <code>org.foo.Handler("abc")</code> asks
	 * for property "level", this method will first look at 
	 * <code>org.foo.Handler("abc").level</code>, then will look at
	 * <code>org.foo.Handler.level</code>.
	 * @return
	 */
	protected String getProperty(String prop) {
		LogManager mgr = LogManager.getLogManager();
		if (name.length() > 0) {
			String key = String.format("%s(\"%s\").%s", getClass().getName(), name, prop);			
			String val = mgr.getProperty(key);
			if (val != null)
				return val;			
		}		
		return mgr.getProperty(getClass().getName() + "." + prop);			
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		if (name.length() == 0) {
			return getClass().getName();
		} else {
			return getClass().getName() + "(\"" + name + "\")";
		}
	}
	
}
