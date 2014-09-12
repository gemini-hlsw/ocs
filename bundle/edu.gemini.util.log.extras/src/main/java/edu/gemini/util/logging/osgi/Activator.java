package edu.gemini.util.logging.osgi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.gemini.util.logging.NamedHandler;

public class Activator implements BundleActivator, PropertyChangeListener {

	private static final String XHANDLER = "xhandlers";
	private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());
	private static final Pattern DELIM = java.util.regex.Pattern.compile("\\s*,\\s*");
	
	public void start(BundleContext arg0) throws Exception {
		LogManager.getLogManager().addPropertyChangeListener(this);
		open();
	}

	public void stop(BundleContext arg0) throws Exception {
		LogManager.getLogManager().removePropertyChangeListener(this);
		close();
	}

	public void propertyChange(PropertyChangeEvent pce) {
		try {
			close();
			open();
		} catch (IOException ioe) {
			LOGGER.log(Level.WARNING, "Could not initialize extended logging.", ioe);
		}
	}
	
	private void open() throws IOException {
		for (Map.Entry<String, String> entry: getHandlerMap().entrySet()) {
			addHandler(entry.getKey(), entry.getValue());
		}
	}
	
	
	private void close() throws IOException {
		for (Map.Entry<String, String> entry: getHandlerMap().entrySet()) {
			removeHandler(entry.getKey(), entry.getValue());
		}
	}
	
	
	private Map<String, String> getHandlerMap() throws IOException {
		Map<String, String> ret = new TreeMap<String, String>();
		for (Map.Entry entry : getProperties().entrySet()) {			
			String property = (String) entry.getKey();			
			if (property.endsWith(XHANDLER)) {
				int pos = property.lastIndexOf(".");
				String loggerName = (pos < 1) ? "" : property.substring(0, pos); 
				for (String handlerDecl: DELIM.split((String) entry.getValue()))
					ret.put(loggerName, handlerDecl);
			}		
		}	
		return ret;
	}
	
	private Properties getProperties() throws IOException {
		File file;
		String path = System.getProperty("java.util.logging.config.file");
		if (path != null) {
			file = new File(path);
		} else {
			File dir = new File(System.getProperty("java.home"), "lib");
			file = new File(dir, "logging.properties");
		}
		
		LOGGER.fine("Examining logging properties: " + file.getPath());
		
		Properties props = new Properties();
		props.load(new FileInputStream(file));
		return props;
	}
	
	private void addHandler(String loggerName, String handlerDecl) {
		LOGGER.info("Adding log handler " + handlerDecl + " to logger \"" + loggerName + "\"");
		try {
			Handler handler = getHandler(handlerDecl);
			Logger logger = Logger.getLogger(loggerName); 			
			logger.addHandler(handler);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Could not add log handler " + handlerDecl + " to logger \"" + loggerName + "\"", e);
		}
	}
	
	private void removeHandler(String loggerName, String handlerDecl) {
		LOGGER.info("Removing log handler " + handlerDecl + " from logger \"" + loggerName + "\"");
		try {
			Class handlerClass = getHandlerClass(handlerDecl);
			String name = getHandlerName(handlerDecl);
			Logger logger = Logger.getLogger(loggerName);	
			
			for (Handler h: logger.getHandlers()) {
				if (handlerClass.isInstance(h)) {
					if (name != null && h instanceof NamedHandler) {
						if (name.equals(((NamedHandler) h).getName())) {
							logger.removeHandler(h);
							return;
						}
					} else {
						logger.removeHandler(h);
						return;
					}
				}
			}
			
			LOGGER.warning("Could not find handler " + handlerDecl + " on logger \"" + loggerName + "\" ... hmm.");
			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Could not add remove handler " + handlerDecl + " from logger \"" + loggerName + "\"", e);
		}
	}

	
	private Handler getHandler(String handlerDecl) throws ClassNotFoundException, ParseException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Class handlerClass = getHandlerClass(handlerDecl);
		String handlerName = getHandlerName(handlerDecl);
		if (handlerName == null) {
			return (Handler) handlerClass.newInstance();
		} else {
			Constructor ctor = handlerClass.getConstructor(String.class);
			return (Handler) ctor.newInstance(handlerName);
		}
	}
	
	private Class getHandlerClass(String handlerDecl) throws ClassNotFoundException {
		int pos = handlerDecl.indexOf("(\"");
		String cname = (pos == -1) ? handlerDecl : handlerDecl.substring(0, pos);
		return Class.forName(cname);
	}

	private String getHandlerName(String handlerDecl) throws ParseException {
		int pos = handlerDecl.indexOf("(\"");
		if (pos == -1)
			return null;
		int pos2 = handlerDecl.indexOf("\")", pos + 1);
		if (pos2 == -1) 
			throw new ParseException("Malformed handlerDecl: " + handlerDecl, pos2);
		return handlerDecl.substring(pos + 2, pos2);
	}
	
}
