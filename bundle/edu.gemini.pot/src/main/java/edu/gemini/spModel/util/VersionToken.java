package edu.gemini.spModel.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public final class VersionToken implements Comparable<VersionToken>, Iterator<VersionToken>, Serializable {

    public static VersionToken apply(int[] segments, int nextSegment) {
        final VersionToken vt = new VersionToken(Arrays.copyOf(segments, segments.length));
        vt.next = nextSegment;
        return vt;
    }

    private final int[] segments;
    private int next = 1;
    private transient String stringValue;

    private VersionToken(int[] segments) {
        if (segments == null || segments.length < 1)
            throw new IllegalArgumentException("Segments must be non-null and non-empty.");
        this.segments = segments;
    }

    public VersionToken(int value) {
        this(new int[]{value});
    }

    public int getFirstSegment() {
        return segments[0];
    }

    public int length() {
        return segments.length;
    }

    public int[] getSegments() {
        return Arrays.copyOf(segments, segments.length);
    }

    public synchronized VersionToken next() {
        final int[] newValues = Arrays.copyOf(segments, segments.length + 1);
        newValues[newValues.length - 1] = next++; // NOTE MUTATION HERE
        return new VersionToken(newValues);
    }

    public int nextSegment() {
        return next;
    }

    public boolean hasNext() {
        return true;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        if (stringValue == null) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0)
                    sb.append('.');
                sb.append(segments[i]);
            }
            stringValue = sb.toString();
        }
        return stringValue;
    }

    public boolean equals(Object o) {
        if (o instanceof VersionToken) {
            final VersionToken t = (VersionToken) o;
            return Arrays.equals(segments, t.segments) && next == t.next;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(segments) ^ next;
    }

    @Override
    public int compareTo(VersionToken that) {
        final int len = Math.min(segments.length, that.segments.length);
        for (int i = 0; i < len; ++i) {
            final int comp = segments[i] - that.segments[i];
            if (comp != 0) return comp;
        }

        final int comp = segments.length - that.segments.length;
        if (comp != 0) return comp;

        return next - that.next;
    }

    public static VersionToken valueOf(String stringValue) {
        return new VersionToken(segments(stringValue));
    }

    public static int[] segments(String stringValue) {
        final String[] strings = stringValue.split("\\.");
        final int[] segments = new int[strings.length];
        for (int i = 0; i < strings.length; i++)
            segments[i] = Integer.valueOf(strings[i]);
        return segments;
    }

//    public static void main(String[] args) {
//        final LinkedList<VersionToken> list = new LinkedList<VersionToken>();
//        list.add(new VersionToken(3));
//        for (int i = 0; i < 50; i++) {
//            final VersionToken vt = list.removeFirst();
//            final VersionToken vt0 = valueOf(vt.toString());
//            System.out.println(vt + " => " + vt.equals(vt0));
//            for (int j = 0; j < 3; j++)
//                list.addLast(vt.next());
//        }
//    }

}
