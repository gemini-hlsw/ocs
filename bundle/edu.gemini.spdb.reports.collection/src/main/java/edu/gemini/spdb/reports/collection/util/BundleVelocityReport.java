package edu.gemini.spdb.reports.collection.util;

import edu.gemini.spdb.reports.util.VelocityReport;

public abstract class BundleVelocityReport extends VelocityReport {

	protected String getResourcePath(String classRelativePath) {
		StringBuilder sb = new StringBuilder("/");
		String[] parts = getClass().getName().split("\\.");
		for (int i = 0; i < parts.length - 1; i++) {
			sb.append(parts[i]);
			sb.append("/");
		}
		return sb.append(classRelativePath).toString();
	}


}
