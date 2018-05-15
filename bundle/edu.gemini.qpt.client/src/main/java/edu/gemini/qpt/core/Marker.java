package edu.gemini.qpt.core;

import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.Union;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Site;

/**
 * Represents a problem or annotation associated with a model object. 
 * @author rnorris
 */
public class Marker implements Comparable<Marker> {

	public enum Severity {
		Error, Warning, Notice, Info
	}
	
	private final Object owner;
	private final Object[] path;
	private final String text;
	private final Severity severity;
	private final boolean qcOnly;
	private final Option<Union<Interval>> union;
	
	public Marker(boolean qcOnly, Object owner, Severity severity, String text, Option<Union<Interval>> union, Object... path) {
		this.qcOnly = qcOnly;
		this.owner = owner;
		this.path = path;
		this.text = text;
		this.severity = severity;
		this.union = union;
	}

	public Marker(boolean qcOnly, Object owner, Severity severity, String text, Object... path) {
		this(qcOnly, owner, severity, text, None.instance(), path);
	}

	public boolean isQcOnly() {
		return qcOnly;
	}
	
	public Object getOwner() {
		return owner;
	}

	public Object[] getPath() {
		return path;
	}

	public Object getTarget() {
		return path[path.length - 1];
	}
	
	public String getText() {
		return text;
	}

    public String getFilteredText() {
        return text.replace("\u00B0", "&deg;");
    }

	private static final String formatInterval(Site site, Interval interval) {
		return String.format(
				"%s - %s",
				TimePreference.format(site, interval.getStart(), "HH:mm"),
				TimePreference.format(site, interval.getEnd(), "HH:mm")
		);
	}

    public String getUnionText(Site site) {

		final Union<Interval> intervals = this.getUnion().getOrElse(new Union<>());

		final ImList<Interval> is = DefaultImList.create(intervals.getIntervals());

		String msg;
		if (is.nonEmpty()) {
			final String m = is.map(i -> formatInterval(site, i)).mkString(" ", ", ", ".");
			msg = this.getText() + m;
		} else {
			msg = this.getText();
		}
		return msg;
	}

    public Severity getSeverity() {
		return severity;
	}

	public Option<Union<Interval>> getUnion() { return union; }

	@Override
	public String toString() {
		return text;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(Marker o) {
		
		// Sort first on severity
		int diff = severity.compareTo(o.severity);
		if (diff != 0) return diff;
		
		// Next sort by target, if they are the same type and
		// are comparable.
		Object t1 = getTarget();
		Object t2 = o.getTarget();
		if (t1 instanceof Comparable && t1.getClass().isInstance(t2) && !t1.equals(t2))
			return ((Comparable) t1).compareTo(t2);
			
		// Otherwise order predictably but arbitrarily, grouping by target.
		diff = t1.toString().compareTo(t2.toString());
		return (diff == 0) ? System.identityHashCode(this) - System.identityHashCode(o) : diff;
		
	}
		
}
