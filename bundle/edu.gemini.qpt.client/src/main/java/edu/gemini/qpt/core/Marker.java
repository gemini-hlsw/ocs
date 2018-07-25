package edu.gemini.qpt.core;

import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.shared.util.immutable.*;
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
    private final ImList<Long> timestamps;

    public Marker(boolean qcOnly, Object owner, Severity severity, String text, ImList<Long> timestamps, Object... path ) {
        this.qcOnly = qcOnly;
        this.owner = owner;
        this.path = path;
        this.text = text;
        this.severity = severity;
        this.timestamps = timestamps;
    }

    public Marker(boolean qcOnly, Object owner, Severity severity, String text, Object... path) {
        this(qcOnly, owner, severity, text, ImCollections.emptyList(), path);
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

    public String getUnionText(Site site) {
        return getUnionText(site, TimePreference.BOX.get());
    }

    public String getUnionText(Site site, TimePreference p) {
        return timestamps.foldLeft(
            text, (s, t) -> s.replaceFirst("%t", p.format(site, t, "HH:mm"))
        );
    }

    public String getFilteredUnionText(Site site, TimePreference p) {
        return this.getUnionText(site, p).replace("\u00B0", "&deg;");
    }

    public Severity getSeverity() {
        return severity;
    }

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
