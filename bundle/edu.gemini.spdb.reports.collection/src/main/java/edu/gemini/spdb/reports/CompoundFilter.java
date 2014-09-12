package edu.gemini.spdb.reports;

import java.util.Map;

public class CompoundFilter implements IFilter {

	private static final long serialVersionUID = 1L;
	private final IFilter[] filters;
	
	public CompoundFilter(IFilter... filters) {
		this.filters = filters;
	}

	public boolean accept(Map<IColumn, ?> row) {
		for (IFilter f: filters) if (f != null && !f.accept(row)) return false;
		return true;
	}
	
}
