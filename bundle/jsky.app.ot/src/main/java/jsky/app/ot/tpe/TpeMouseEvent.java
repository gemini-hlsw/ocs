package jsky.app.ot.tpe;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.SiderealTarget;
import jsky.coords.WorldCoords;

import java.awt.event.MouseEvent;

/**
 * A mouse event that occurred in the TpeImageWidget widget.  This structure
 * contains fields that describe the type of mouse event, and the location
 * relative to the image being viewed by the image widget.
 * The x and y fields are stored as doubles since the image may be scaled.
 * The ra and dec where the event occurred and the x and y offset in arcsec
 * from the base position are also stored here.
 */
public class TpeMouseEvent {

    /** The original mouse event  */
    public final MouseEvent mouseEvent;

    /** Event id, same as java.awt.Event.  */
    public final int id;

    /** Event source.  */
    public final Option<TpeImageWidget> source;

    /** X pos of event relative to the widget.  */
    public final int xWidget;

    /** Y pos of event relative to the widget.  */
    public final int yWidget;

    /** The world coordinate position */
    public final Coordinates pos;

    /** Optional object name or id */
    public final Option<String> name;

    // Optional details of the object.
    public final Option<SiderealTarget> skyObject;

    /** The X offset of the event from the base position in arcsec. */
    public final double xOffset;

    /** The Y offset of the event from the base position in arcsec. */
    public final double yOffset;

    /** Default Constructor: initialize all fields to null. */
    public TpeMouseEvent(MouseEvent e) {
        mouseEvent = e;
        this.id = 0;
        this.source = None.instance();
        this.pos = Coordinates.zero();
        this.name = None.instance();
        this.skyObject = None.instance();
        this.xWidget = 0;
        this.yWidget = 0;
        this.xOffset = 0;
        this.yOffset = 0;
    }

    /** Default Constructor: initialize all fields to null. */
    public TpeMouseEvent(MouseEvent e, int id, Option<TpeImageWidget> source, Coordinates pos, Option<String> name, int xWidget, int yWidget, Option<SiderealTarget> skyObject, double xOffset, double yOffset) {
        mouseEvent = e;
        this.id = id;
        this.source = source;
        this.pos = pos;
        this.name = name;
        this.xWidget = xWidget;
        this.yWidget = yWidget;
        this.skyObject = skyObject;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    /**
     * Returns a human-readable string describing the contents of
     * the event.
     */
    public String toString() {
        String event = "<unknown: " + id + ">";
        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                event = "MOUSE_PRESSED";
                break;
            case MouseEvent.MOUSE_RELEASED:
                event = "MOUSE_RELEASED";
                break;
            case MouseEvent.MOUSE_MOVED:
                event = "MOUSE_MOVED";
                break;
            case MouseEvent.MOUSE_DRAGGED:
                event = "MOUSE_DRAGGED";
                break;
        }
        return "TpeMouseEvent[id=" + event
                + ", xWidget=" + xWidget
                + ", yWidget=" + yWidget
                + ", pos=" + pos
                + ", name=" + (name != null ? name : "<null>")
                + ", xOffset=" + xOffset
                + ", yOffset=" + yOffset + "]";
    }
}
