package edu.gemini.spdb.reports.osgi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * An HttpContext to serve batch report files from the bundle's data directory.
 * @author rnorris
 */
public class ReportContext implements HttpContext {

	private static final Logger LOGGER = Logger.getLogger(ReportContext.class.getName());
	private final String root;
	
	public ReportContext(URL root) {
		this.root = root.toString();
	}
	
	public boolean handleSecurity(HttpServletRequest req, HttpServletResponse res) throws IOException {
		return true;
	}

	public URL getResource(String path) {
		try {			
			URL ret = new URL(root + path.substring(1));
			LOGGER.fine("Requested " + ret);
			return ret;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getMimeType(String path) {
		return null;
	}

}
