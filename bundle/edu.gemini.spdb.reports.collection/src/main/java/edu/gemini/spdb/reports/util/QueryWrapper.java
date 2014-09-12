package edu.gemini.spdb.reports.util;

import java.util.List;
import java.util.NoSuchElementException;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IFilter;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.ITable;

/**
 * Convenience class that wraps an IQuery and provides methods for manipulating
 * its collection properties in a more fine-grained manner.
 * @author rnorris
 *
 */
public class QueryWrapper implements IQuery {

	private static final long serialVersionUID = 1L;

	private final IQuery query;

	public QueryWrapper(IQuery query) {
		this.query = query;
	}

	public List<IColumn> getOutputColumns() {
		return query.getOutputColumns();
	}

	public List<ISort> getGroups() {
		return query.getGroups();
	}

	public void setGroups(ISort... groups) {
		query.setGroups(groups);
	}

	public List<ISort> getSorts() {
		return query.getSorts();
	}

	public ITable getTable() {
		return query.getTable();
	}

	public IFilter getFilter() {
		return query.getFilter();
	}

	public void setFilter(IFilter filter) {
		query.setFilter(filter);
	}

	public void setOutputColumns(IColumn... col) {
		query.setOutputColumns(col);
	}

	public void setSorts(ISort... sort) {
		query.setSorts(sort);
	}

	@SuppressWarnings("unchecked")
	public void addOutputColumn(String sid) {
		List<IColumn> list = getOutputColumns();
		IColumn[] cols = list.toArray(new IColumn[list.size() + 1]);
		for (IColumn col: getTable().getColumns()) {
			if (col.name().equals(sid)) {
				cols[cols.length - 1] = col;
				break;
			}
		}
		setOutputColumns(cols);
	}
	
	@SuppressWarnings("unchecked")
	public void deleteOutputColumn(String sid) {
		IColumn[] cols = getOutputColumns().toArray(new IColumn[0]);
		IColumn[] out = new IColumn[cols.length - 1];
		int j = 0;
		for (int i = 0; i < cols.length; i++) {
			if (!cols[i].name().equals(sid))
				out[j++] = cols[i];
		}
		setOutputColumns(out);
	}

	@SuppressWarnings("unchecked")
	public void swapOutputColumn(String sid, int delta) {
		IColumn[] cols = getOutputColumns().toArray(new IColumn[0]);
		for (int i = 0; i < cols.length; i++) {
			if (cols[i].name().equals(sid)) {
				IColumn temp = cols[i];
				cols[i] = cols[i + delta];
				cols[i + delta] = temp;
				query.setOutputColumns(cols);
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addSort(ISort sort) {
		List<ISort> list = getSorts();
		ISort[] sorts = list.toArray(new ISort[list.size() + 1]);
		sorts[sorts.length - 1] = sort;
		setSorts(sorts);
	}

	@SuppressWarnings("unchecked")
	public void addGroup(ISort sort) {
		List<ISort> list = getGroups();
		ISort[] sorts = list.toArray(new ISort[list.size() + 1]);
		sorts[sorts.length - 1] = sort;
		setGroups(sorts);
	}

	@SuppressWarnings("unchecked")
	public void deleteSort(String sid) {
		ISort[] sorts = getSorts().toArray(new ISort[0]);
		ISort[] out = new ISort[sorts.length - 1];
		int j = 0;
		for (int i = 0; i < sorts.length; i++) {
			if (!sorts[i].getColumn().name().equals(sid))
				out[j++] = sorts[i];
		}
		setSorts(out);
	}
	
	@SuppressWarnings("unchecked")
	public void deleteGroup(String sid) {
		ISort[] sorts = getGroups().toArray(new ISort[0]);
		ISort[] out = new ISort[sorts.length - 1];
		int j = 0;
		for (int i = 0; i < sorts.length; i++) {
			if (!sorts[i].getColumn().name().equals(sid))
				out[j++] = sorts[i];
		}
		setGroups(out);
	}
	
	@SuppressWarnings("unchecked")
	public void swapSort(String sid, int delta) {
		ISort[] sorts = getSorts().toArray(new ISort[0]);
		for (int i = 0; i < sorts.length; i++) {
			if (sorts[i].getColumn().name().equals(sid)) {
				ISort temp = sorts[i];
				sorts[i] = sorts[i + delta];
				sorts[i + delta] = temp;
				query.setSorts(sorts);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void swapGroup(String sid, int delta) {
		ISort[] sorts = getGroups().toArray(new ISort[0]);
		for (int i = 0; i < sorts.length; i++) {
			if (sorts[i].getColumn().name().equals(sid)) {
				ISort temp = sorts[i];
				sorts[i] = sorts[i + delta];
				sorts[i + delta] = temp;
				query.setGroups(sorts);
				break;
			}
		}
	}

//	@SuppressWarnings("unchecked")
//	public void setSortOrder(String sid, Order order) {
//		ISort[] sorts = getSorts().toArray(new ISort[0]);
//		for (int i = 0; i < sorts.length; i++) {
//			if (sorts[i].getColumn().getString().equals(sid)) {
//				sorts[i].setOrder(order);
//				break;
//			}
//		}
//	}

	@SuppressWarnings("unchecked")
	public ISort getSort(String sid) {
		ISort[] sorts = getSorts().toArray(new ISort[0]);
		for (int i = 0; i < sorts.length; i++) {
			if (sorts[i].getColumn().name().equals(sid)) {
				return sorts[i];
			}
		}
		throw new NoSuchElementException(sid.toString());
	}

	@SuppressWarnings("unchecked")
	public ISort getGroup(String sid) {
		ISort[] sorts = getGroups().toArray(new ISort[0]);
		for (int i = 0; i < sorts.length; i++) {
			if (sorts[i].getColumn().name().equals(sid)) {
				return sorts[i];
			}
		}
		throw new NoSuchElementException(sid.toString());
	}

//	@SuppressWarnings("unchecked")
//	public void setSortNullPolicy(String sid, NullPolicy policy) {
//		ISort[] sorts = getSorts().toArray(new ISort[0]);
//		for (int i = 0; i < sorts.length; i++) {
//			if (sorts[i].getColumn().getString().equals(sid)) {
//				sorts[i].setNullPolicy(policy);
//				break;
//			}
//		}
//	}
//
	
	@SuppressWarnings("unchecked")
	public boolean containsSort(String sid) {
		ISort[] sorts = query.getSorts().toArray(new ISort[0]);
		for (int i = 0; i < sorts.length; i++) {
			if (sorts[i].getColumn().name().equals(sid)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean containsGroup(String sid) {
		ISort[] sorts = query.getGroups().toArray(new ISort[0]);
		for (int i = 0; i < sorts.length; i++) {
			if (sorts[i].getColumn().name().equals(sid)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked") 
	public IColumn getColumn(String sid) {
		for (IColumn col: getTable().getColumns()) {
			if (col.name().equals(sid))
				return col;
		}
		throw new NoSuchElementException(sid.toString());
	}
	
}

