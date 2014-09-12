package edu.gemini.spdb.reports.impl;

import java.util.TreeMap;

import edu.gemini.spdb.reports.ITable;

/**
 * Trivial ITable registry; just a singleton Map from String to ITable.
 * @author rnorris
 */
@SuppressWarnings("serial")
public class TableManager extends TreeMap<String, ITable>  {

	private static final TableManager INSTANCE = new TableManager();
	
	private TableManager() {
		// no public creation
	}
	
	public static TableManager getInstance() {
		return INSTANCE;
	}
		
}
