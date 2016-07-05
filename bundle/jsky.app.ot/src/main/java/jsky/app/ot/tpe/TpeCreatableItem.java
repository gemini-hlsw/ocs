package jsky.app.ot.tpe;


/**
 * An interface for the items that can be created by features that support
 * the {@link TpeCreatableFeature} interface.  A single feature may support
 * creating multiple items.
 */
public interface TpeCreatableItem {

    /**
     * The types of items that can be created.
     */
    enum Type {
        wfsTarget,
        userTarget,
        offsetPosition
    }

    /**
     * Label that identifies the item (used as a label button in the TPE).
     */
    String getLabel();

    /**
     * Gets the type of creatable item.
     */
    Type getType();

    /**
     * Whether to enable or disable the creation of this kind of item based
     * upon the current SPProgData.  Here, the creatable item is likely to
     * check on the availability of guiders in the current context.
     *
     * @return <code>true</code> if the item can be created
     */
    boolean isEnabled(TpeContext ctx);

    /**
     * Creates the item using the coordinates of the mouse (as described in
     * the {@link TpeMouseEvent}.
     */
    void create(TpeMouseEvent tme, TpeImageInfo tii);
}
