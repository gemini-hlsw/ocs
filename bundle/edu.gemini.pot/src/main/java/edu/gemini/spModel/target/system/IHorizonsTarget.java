package edu.gemini.spModel.target.system;

/**
 * This interface provides an indirect way of storing Horizons query information
 * in the core target model, without creating a direct dependency between SPModel
 * and Horizons-API. This may turn out to have been a poor design decision.
 * <p>
 * An ITarget that also implements IHorizonsTarget provides two additional members
 * that can be used to create a HorizonsQuery, which can then be used to look up
 * the target's ephemeris for any point in time without user intervention: <pre>
 *    Site site = ...
 *    ITarget t = ...
 *    if (t instanceof IHorizonsTarget) {
 *    	IHorizonsTarget ht = (IHorizonsTarget) t;
 *    	if (ht.isHorizonsDataPopulated()) {
 *          HorizonsQuery hq = new HorizonsQuery(site);
 *          hq.setObjectId(ht.getHorizonsObjectId());
 *          hq.setObjectType(HorizonsQuery.ObjectType.valueOf(hq.getHorizonsObjectTypeOrdinal()));
 *           ...
 *          // Execute the query here
 *    	}
 *    }</pre>Note that although you should normally expect a single result, you
 * still need to double-check to be sure the query did what you expected (it is
 * reasonable to treat a failure as an exception condition). We reverse-engineered
 * the format of Horizons responses, so there's no telling how they may change in
 * the future, and some targets may come back in formats we haven't seen before.
 * @author rnorris
 */
public interface IHorizonsTarget {

	/** PIO keys for the horizons object ID; used in XML serialization. */
	String PK_HORIZONS_OBJECT_ID = "horizons-object-id";

	/** PIO keys for the horizons object type ordinal; used in XML serialization. */
	String PK_HORIZONS_OBJECT_TYPE_ORDINAL = "horizons-object-type";

	/**
	 * Returns true if the object ID and ordinal are populated, or
	 * false otherwise. If true, we can use this object to construct a
	 * HorizonsQuery.
	 * @return true if the object ID and ordinal are populated.
	 */
	boolean isHorizonsDataPopulated();

	/**
	 * Returns the Horizons object ID, or null if unpopulated.
	 * @return the Horizons object ID, or null.
	 */
	Long getHorizonsObjectId();

	/**
	 * Sets the Horizons object ID. May be set to null.
	 * @param id a new object ID, or null
	 */
	void setHorizonsObjectId(Long id);

	/**
	 * Returns the <code>ordinal()</code> value of this object's
	 * <code>HorizonsQuery.ObjectType</code>, or <code>-1</code> if the
	 * type is unknown. Pass this value to <code>HorizonsQuery.ObjectType.valueOf()</code>
	 * in order to get the actual enum value. This indirection is needed to
	 * avoid a dependency on Horizons-API.
	 * @return a <code>HorizonsQuery.ObjectType</code> ordinal value, or -1 if unknown
	 */
	int getHorizonsObjectTypeOrdinal();

	/**
	 * Sets the Horizons object type ordinal, which should be the <code>ordinal()</code>
	 * of a <code>HorizonsQuery.ObjectType</code>, or <code>-1</code> to indicate that
	 * the type is unknown. Note that you'll normally be populating
	 * this value from a <code>HorizonsReply</code>, so you'll need to say
	 * <code>reply.getReplyType().objectType().ordinal()</code>.
	 * @param ord the new ordinal value, or -1 to indicate the value is unknown
	 */
	void setHorizonsObjectTypeOrdinal(int ord);

}
