//
// $Id: PioFactory.java 4937 2004-08-14 21:35:20Z shane $
//
package edu.gemini.spModel.pio;

/**
 * The PIO package consists primarily of interfaces.  Concrete implementation
 * objects are obtained through implementation of this factory class.
 */
public interface PioFactory {

    /**
     * Creates the Document that will hold Containers.
     */
    Document createDocument();

    /**
     * Creates a Container and gives it its {@link Container#getKind kind},
     * {@link Container#getType type}, and {@link Container#getVersion version}.
     *
     * @param kind {@link Container#getKind kind} of the Container; may
     * <em>not</em> be <code>null</code>
     * @param type {@link Container#getType type} of the Container; may
     * <em>not</em> be <code>null</code>
     * @param version {@link Container#getVersion version} of the Container;
     * may <em>not</em> be <code>null</code>
     *
     * @return a Container instance with its kind, type, and version set
     *
     * @throws NullPointerException if <code>kind</code>, <code>type</code>, or
     * <code>version</code> is <code>null</code>
     */
    Container createContainer(String kind, String type, String version);

    /**
     * Creates a ParamSet and gives it a name.
     *
     * @param name name for the ParamSet; may <em>not</em> be <code>null</code>
     *
     * @return a ParamSet instance with its name set to <code>name</code>
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    ParamSet createParamSet(String name);

    /**
     * Creates a Param and gives it a name.
     *
     * @param name name for the Param; may <em>not</em> be <code>null</code>
     *
     * @return a Param instance with its name set to <code>name</code>
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    Param createParam(String name);
}
