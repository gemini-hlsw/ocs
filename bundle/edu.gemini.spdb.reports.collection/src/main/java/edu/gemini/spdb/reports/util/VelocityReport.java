package edu.gemini.spdb.reports.util;

import edu.gemini.spdb.reports.IReport;
import edu.gemini.spdb.reports.impl.www.DelegatingLogSystem;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Base class for a report that uses Velocity to generate its output.
 * @author rnorris
 */
public abstract class VelocityReport implements IReport {

	private final VelocityEngine ve;

	public VelocityReport() {
		try {
			ve = new VelocityEngine();
			ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "context");
			ve.setProperty("context.resource.loader.class", ContextClasspathResourceLoader.class.getName());
			ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, DelegatingLogSystem.class.getName());
			ve.init();
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Create <b>file</b> using the template at <b>path</b> and the environment
	 * <b>map</b>. The template path will be evaluated by the concrete
	 * report's Classloader.
	 * @param file an output file
	 * @param path a path to be evaluated by the classloader
	 * @param map a binding environment
	 * @throws IOException
	 */
	protected void merge(final File file, String path, final Map map) throws IOException {

		final FileOutputStream fos = new FileOutputStream(file);
		final OutputStreamWriter osw = new OutputStreamWriter(fos);

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			ve.mergeTemplate(path, "UTF-8", new VelocityContext(map), osw);
		} catch (IOException | RuntimeException ioe) {
			throw ioe;
		} catch (Exception e) {
			throw new IOException(e.toString());
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}

		osw.flush();
		fos.flush();
		fos.close();
	}

}

