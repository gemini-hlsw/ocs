package edu.gemini.spdb.reports.impl;

import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IFilter;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.ITable;

/**
 * SPDB functor that can execute IQuery definitions and return results.
 * Internal to the implementation.
 * @author rnorris
 */
class QueryFunctor extends DBAbstractQueryFunctor {

	static final Logger LOGGER = Logger.getLogger(QueryFunctor.class.getName());
	private static final long serialVersionUID = 1L;

	/** The query we're going to execute. */
	private final IQuery query;

	/**
	 * Storage for raw rows that we accumulate during execute() and collate
	 * in finished(). The collection is cleared before the functor returns,
	 * so it will always be empty as far as the client code is concerned.
	 */
	private final List<Map<IColumn, ?>> rows = new ArrayList<Map<IColumn, ?>>();

	/**
	 * Storage for returned rows, which get serialized and returned to the
	 * client.
	 */
	private final List<IRow> results = new ArrayList<IRow>();

	/**
	 * Construct a functor for the specified query, which is final.
	 * @param query
	 */
	QueryFunctor(IQuery query) {
		this.query = query;
	}

    /**
     * Run the reports server QueryFunctor in low priority.
     */
    @Override public Priority getPriority() { return Priority.low; }

    /**
	 * Init function simply clears leftover results.
	 */
	@Override
	public void init() {
		results.clear();
	}

	/**
	 * Delegate to getRows() and filter immediately, accumulating non-
	 * filtered rows as we go.
	 */
	@SuppressWarnings("unchecked")
	public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
		try {
			ITable table = query.getTable();
			for (Map<IColumn, ?> row: table.getRows(node)) {
				IFilter f = query.getFilter();
				if (f == null || f.accept(row))
					rows.add(row);
			}
			Thread.yield(); // Try not to bog down the server.
		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE, "Problem executing functor.", t);
		}
	}

	/**
	 * Group and sort the raw rows, then create IRows to return.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void finished() {
		try {

			// Group and sort the rows.
			Map<IColumn, ?>[] rowArray = rows.toArray(new Map[rows.size()]);
			RowComparator rc = new RowComparator();
			for (ISort s: query.getGroups()) rc.addComparator(s);
			for (ISort s: query.getSorts()) rc.addComparator(s);
			Arrays.sort(rowArray, rc);

			// And turn them into IRows.
			final IColumn[] outputColumns = query.getOutputColumns().toArray(new IColumn[0]);
			final ISort[] groups = query.getGroups().toArray(new ISort[0]);
			IRow prev = null;
			for (Map<IColumn, ?> row: rowArray) {

				// Values
				Object[] values = new Object[outputColumns.length];
				for (int i = 0; i < outputColumns.length; i++) {
					IColumn col = outputColumns[i];
					Object val = row.get(col);
					values[i] = val == null ? IRow.NULL_VALUE : val;
				}

				// GroupValues and GroupIndex
				int groupIndex = (prev == null && groups.length > 0) ? 0 : -1;
				Object[] groupValues = new Object[groups.length];
				for (int i = 0; i < groups.length; i++) {
					IColumn col = groups[i].getColumn();
					Object val = row.get(col);
					if (groupIndex == -1 && (!equiv(val, prev.getGroupValues()[i])))
						groupIndex = i;
					groupValues[i] = val == null ? IRow.NULL_VALUE : val;
				}

				// Done.
				results.add(prev = new Row(values, groupValues, groupIndex));

				// Let the server breathe.
				Thread.yield();

			}

			// Clean up a little. Don't need this anymore.
			rows.clear();

		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE, "Problem finishing functor.", t);
		}
	}

	private static boolean equiv(Object a, Object b) {
		return (a == b) || (a != null && a.equals(b));
	}

	public List<IRow> getResults() {
		return results;
	}

	/**
	 * Trivial implementation of IRow.
	 * @author rnorris
	 */
	static class Row implements IRow {

		private static final long serialVersionUID = 1L;

		private final Object[] values;
		private final Object[] groupValues;
		private final int groupIndex;

		public Row(Object[] values, Object[] groupValues, int groupIndex) {
			this.values = values;
			this.groupValues = groupValues;
			this.groupIndex = groupIndex;
		}

		public int getGroupIndex() {
			return groupIndex;
		}

		public Object[] getGroupValues() {
			return groupValues;
		}

		public Object[] getValues() {
			return values;
		}

		public Object getValue(int i) {
			return values[i];
		}

		public Object getGroupValue(int i) {
			return groupValues[i];
		}

	}

}


