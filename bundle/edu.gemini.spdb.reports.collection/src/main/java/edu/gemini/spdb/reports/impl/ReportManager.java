package edu.gemini.spdb.reports.impl;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.gemini.spdb.reports.IReport;

/**
 * Trivial registry for IReport services.
 * @author rnorris
 */
@SuppressWarnings("serial")
public class ReportManager implements Iterable<ReportManager.ReportRegistration> {

	private static final ReportManager INSTANCE = new ReportManager();
	
	private final Map<String, ReportRegistration> map = new TreeMap<>();
	
	private ReportManager() {
		// no public creation
	}
	
	public static ReportManager getInstance() {
		return INSTANCE;
	}

	public void add(String id, String tableId, IReport report) {
		map.put(id, new ReportRegistration(id, tableId, report));
	}
	
	public IReport getReport(String id) {
		ReportRegistration reg = map.get(id);
		return reg != null ? reg.report : null;
	}

	public String getReportTableId(String id) {
		ReportRegistration reg = map.get(id);
		return reg != null ? reg.tableId : null;
	}
	
	public void remove(String id) {
		map.remove(id);
	}

	public final class ReportRegistration {
		
		public final String id;
		public final String tableId;
		public final IReport report;
		private List<File> files = Collections.emptyList();
		
		public List<File> getFiles() {
			return files;
		}

		public void setFiles(List<File> files) {
			this.files = files;
		}

		protected ReportRegistration(String id, String tableId, IReport report) {
			this.id = id;
			this.tableId = tableId;
			this.report = report;
		}
		
		@Override
		public int hashCode() {
			return id.hashCode() ^ tableId.hashCode() ^ report.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj == this;
		}

		public String getId() {
			return id;
		}

		public IReport getReport() {
			return report;
		}

		public String getTableId() {
			return tableId;
		}
		
	}

	public Iterator<ReportRegistration> iterator() {
		return map.values().iterator();
	}

	public Collection<ReportRegistration> getRegistrations() {
		return map.values();
	}
	
}

