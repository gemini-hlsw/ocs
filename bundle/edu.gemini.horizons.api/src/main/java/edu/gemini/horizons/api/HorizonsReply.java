package edu.gemini.horizons.api;

import java.io.Serializable;
import java.util.Vector;
import java.util.List;

/**
 * Representation of an answer from the Horizons Service. Clients usually will need to
 * analyze the <code>ReplyType</code> in this class in order to distinguish among
 * different types of answers that could be returned by the service.
 */
public final class HorizonsReply implements Serializable {
    /**
     * Defines the object Type.
     */
    public enum ObjectType {
        /**
         * A comet
         */
        COMET,
        /**
         * A minor body, usually an asteroid
         */
        MINOR_BODY,
        /**
         * A Major body like a planet or the Moon
         */
        MAJOR_BODY
    }

    /**
     * Definition of the different types of answers that could be returned by a query to
     * the Horizons service
     */
    public enum ReplyType {
        /**
         * A single answer from a comet
         */
        COMET(ObjectType.COMET),

        /**
         * A single answer from a a minor object. Usually
         * a minor object is an asteroid...well, that's what I believe :/
         */
        MINOR_OBJECT(ObjectType.MINOR_BODY),

        /**
         * A single answer from a major planets. Major planets objects don't have orbital
         * elements, but might have ephemeris
         */
        MAJOR_PLANET(ObjectType.MAJOR_BODY),
        /**
         * There were multiple matches for this query. The results are stored in the
         * {@link edu.gemini.horizons.api.ResultsTable} object inside the reply.
         */
        MUTLIPLE_ANSWER,
        /**
         * A Spacecraft. It's possible to get ephemeris for a spacecraft
         */
        SPACECRAFT,
        /**
         * The query didn't return any results
         */
        NO_RESULTS,
        /**
         * Invalid query sent to the server
         */
        INVALID_QUERY,

        ;

        private final ObjectType objectType;

        ReplyType() {
        	this(null);
        }

        ReplyType(ObjectType objectType) {
			this.objectType = objectType;
		}

		/**
         * Returns this ReplyType's corresponding ObjectType, in case we want to use this reply
         * to populate a new query. This value will be null if there is no corresponding object
         * type.
         * @return an ObjectType, or null
         */
        public ObjectType objectType() {
        	return objectType;
        }

    }

    /**
     * Answer Type. Allows client to analyze this reply properly.
     */
    private ReplyType _type = ReplyType.NO_RESULTS;

    /**
     * A vector with all the ephemeris gotten for the particular object (if any) between
     * the specified dates in the {@link edu.gemini.horizons.api.HorizonsQuery} object
     */
    private Vector<EphemerisEntry> _ephemeris;

    /**
     * Orbital elements (if available) for the particular object. In general, major planets don't
     * have orbital elements.
     */
    private OrbitalElements _orbitalElements;

    /**
     * A table with the available options when a unique object can not be found in the server
     * for the given query arguments. This table will contain all the objects that matches
     * the given query and will provide a unique identifier for the objects. Clients can use
     * that identifier to perform the query again, and get the desired result.
     */
    private ResultsTable _resultsTable;

    /**
     * The unique ID string that can be used to re-query this object in the future,
     * or null if the type is INVALID_QUERY, MULTIPLE_ANSWER, or NO_RESULTS.
     */
    private Long _objectId;

    /**
     * Set the type of this reply.
     * @param type Reply Type.
     */
    public void setReplyType(ReplyType type) {
        _type = type;
    }

    /**
     * Get the type of this reply. Allows client to analyze this reply properly. The default
     * value is {@link edu.gemini.horizons.api.HorizonsReply.ReplyType#NO_RESULTS}
     * @return Reply type for this particular object.
     */
    public ReplyType getReplyType() {
        return _type;
    }

    /**
     * Get the Ephemeris for the particular object (if any) between
     * the specified dates in the {@link edu.gemini.horizons.api.HorizonsQuery} object.
     * Clients always should have to analyze the
     * value of {@link edu.gemini.horizons.api.HorizonsReply#hasEphemeris()}
     * before attempting to get the ephemeris
     * @return  The Ephemeris available or an empy vector if there is no one.
     */
    public List<EphemerisEntry> getEphemeris() {
        if (_ephemeris == null) {
            _ephemeris = new Vector<>();
        }
        return _ephemeris;
    }

    /**
     * Add an entry to the ephemeris for this particular object. Entries are appended
     * to the existing list.
     * @param entry The ephemers entry to be added. If <code>null</code>,
     * the entry is discarded.
     */
    public void addEphemerisEntry(EphemerisEntry entry) {
        if (entry != null) {
            getEphemeris().add(entry);
        }
    }

    /**
     * Get the orbital elements for the object, if available. <code>null</code> otherwise.
     * Clients always should have to analyze the
     * value of {@link HorizonsReply#getReplyType()} before attempting to get the orbital
     * elements.
     * @return the orbital elements for the object, if available. <code>null</code> otherwise
     */
    public OrbitalElements getOrbitalElements() {
        return _orbitalElements;
    }

    /**
     * Set the orbital elements for the object being queried.
     * @param elements the orbital elements describing this object orbit.
     */
    public void setOrbitalElements(OrbitalElements elements) {
        _orbitalElements = elements;
    }

    /**
     * Get the table with the multiple matches the query has returned. This table
     * will be available if the {@link HorizonsReply#getReplyType()} is
     * {@link ReplyType#MUTLIPLE_ANSWER}.
     * In other cases, returns <code>null</code>. Clients always have to analyze the
     * value of {@link HorizonsReply#getReplyType()} before attempting to get this object.
     *
     * @return A table with the multiple matches found for an object, if the reply type is
     * {@link ReplyType#MUTLIPLE_ANSWER}. Otherwise, <code>null</null> is returned
     */
    public ResultsTable getResultsTable() {
        return _resultsTable;
    }

    /**
     * Set the table with the multiple matches the query has returned.
     * @param table the table with the matches returned by the query
     */
    public void setResultsTable(ResultsTable table) {
        _resultsTable = table;
    }

    /**
     * Return <code>true</code> if this <code>HorizonReply</code> object
     * contains an ephemeris (a list of {@link edu.gemini.horizons.api.EphemerisEntry})
     * @return true if an ephemeris is found, false otherwise.
     */
    public boolean hasEphemeris() {

        if (_ephemeris == null)  return false;
        if (_ephemeris.size() <= 0) return false;
        //the vector isn't empty, so it has at least one EphemerisEntry.
        return true;
    }

    /**
     * Return <code>true</code> if this <code>HorizonReply</code> object
     * contains a valid {@link edu.gemini.horizons.api.OrbitalElements}
     * @return true if this reply contains <code>OrbitalElements></code>
     */
    public boolean hasOrbitalElements() {
        if (_orbitalElements == null) return false;
        if (_orbitalElements.getKeys().size() <= 0) return false;
        //the orbita elements object contains at least one valid entry.
        return true;
    }

    /**
     * Returns this object's unique id, if available. This number
     * can be used to query this object again in the future.
     * @return object's ID number, or null if not available
     */
    public Long getObjectId() {
		return _objectId;
	}

    /**
     * Sets the object's unique id.
     * @param objectId the new object Id, or null
     */
	public void setObjectId(Long objectId) {
		_objectId = objectId;
	}

	/**
	 * Returns the object type, or null if the not applicable.
	 */
	public ObjectType getObjectType() {
		return _type.objectType;
	}

	/**
	 * Returns true if the object ID is available and the reply type has a corresponding object type.
	 * @return true if the ID and type are available.
	 */
	public boolean hasObjectIdAndType() {
		return _objectId != null && _type.objectType != null;
	}

	public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\n***\nReply Type: ").append(getReplyType()).append('\n');

        if (hasObjectIdAndType()) {
        	buffer.append("Object ID: ").append(getObjectId())
                    .append("\nObject Type: ").append(_type.objectType()).append("\n");
        }

        if (hasOrbitalElements()) {
            buffer.append(getOrbitalElements());
        }

        if (hasEphemeris()) {
            for (EphemerisEntry entry : getEphemeris()) {
                buffer.append("\t").append(entry).append('\n');
            }
        }

        if (getReplyType() == HorizonsReply.ReplyType.MUTLIPLE_ANSWER) {
            buffer.append(getResultsTable()).append("\n");
        }
        return buffer.toString();
    }


    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof HorizonsReply)) return false;

        HorizonsReply that = (HorizonsReply)obj;

        if (this.getReplyType() != that.getReplyType()) return false;

        if (!this.getEphemeris().equals(that.getEphemeris())) return false;

        if (this.getOrbitalElements() == null) {
            if (that.getOrbitalElements() != null) return false;
        } else {
            if (!this.getOrbitalElements().equals(that.getOrbitalElements())) return false;
        }

        if (this.getResultsTable() == null) {
            if (that.getResultsTable() != null) return false;
        } else {
            if (!this.getResultsTable().equals(that.getResultsTable())) return false;
        }

        if (hasObjectIdAndType()) {
        	if (!that.hasObjectIdAndType()) return false;
        	if (!getObjectId().equals(that.getObjectId())) return false;
        	if (!getObjectType().equals(that.getObjectType())) return false;
        } else {
        	if (that.hasObjectIdAndType()) return false;
        }

        return true;
    }

    public int hashCode() {
        int hash = 0;
        if (getReplyType() != null) {
            hash = getReplyType().hashCode();
        }

        if (getEphemeris() != null) {
            hash = 31 * hash + getEphemeris().hashCode();
        }

        if (getOrbitalElements() != null) {
            hash = 31 * hash + getOrbitalElements().hashCode();
        }

        if (getResultsTable() != null) {
            hash = 31 * hash + getResultsTable().hashCode();
        }

        if (hasObjectIdAndType()) {
        	hash = 31 * hash + getObjectId().hashCode();
        	hash = 31 * hash + getObjectType().hashCode(); // not really necessary
        }

        return hash;
    }
}
