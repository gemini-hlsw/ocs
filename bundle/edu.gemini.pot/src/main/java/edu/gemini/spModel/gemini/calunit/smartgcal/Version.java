// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Version implements Comparable<Version>, Serializable {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");

    public static synchronized Version parse(String s) {
        try {
            // TODO: checking for a #-sign can be removed as soon as everybody is using the
            // TODO: OT against the newest servlet version... currently we leave this here for compatibility...
            String[] parts = s.split(",");
            String revisionStr = parts[0].startsWith("#") ? parts[0].substring(1) : parts[0];
            Number revision    = Long.parseLong(revisionStr);
            Date timestamp     = new Date(0,0,1,0,0);
            if (parts.length > 1) {
                // checking for existence of second part will provide backwards compatibility
                // and allow this to work with versions that have no timestamp yet
                timestamp = sdf.parse(parts[1]);
            }
            return new Version(revision, timestamp);
        } catch (Exception e) {
            throw new RuntimeException("could not parse version: '" + s + "'");
        }
    }

    public static synchronized String format(Date timestamp) {
        return sdf.format(timestamp);
    }


    private final Number revision;
    private final Date timestamp;

    public Version(Number revision, Date timestamp) {
        this.revision =  revision;
        this.timestamp = timestamp;
    }

    public Number getRevision() {
        return this.revision;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String toString() {
        return revision.toString() + "," + format(timestamp);
    }


    @Override
    public int compareTo(Version that) {
        return (int) (this.getRevision().longValue() - that.getRevision().longValue());
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version that = (Version) o;

        if (!this.timestamp.equals(that.timestamp)) return false;
        if (!this.revision.equals(that.revision)) return false;

        return true;
    }
}
