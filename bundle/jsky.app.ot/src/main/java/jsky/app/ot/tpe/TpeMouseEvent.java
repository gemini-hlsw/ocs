package jsky.app.ot.tpe;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
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
    public MouseEvent mouseEvent;

    /** Event id, same as java.awt.Event.  */
    public int id;

    /** Event source.  */
    public TpeImageWidget source;

    /** X pos of event relative to the real image being viewed.  */
    public double xView;

    /** Y pos of event relative to the real image being viewed.  */
    public double yView;

    /** X pos of event relative to the widget.  */
    public int xWidget;

    /** Y pos of event relative to the widget.  */
    public int yWidget;

    /** The world coordinate position */
    public WorldCoords pos;

    /** Optional object name or id */
    public String name;

    // Optional details of the object.
    private Option<SiderealTarget> skyObject = None.instance();

    /** The X offset of the event from the base position in arcsec. */
    public double xOffset;

    /** The Y offset of the event from the base position in arcsec. */
    public double yOffset;

    /** Default Constructor: initialize all fields to null. */
    public TpeMouseEvent(MouseEvent e) {
        mouseEvent = e;
    }

    public Option<SiderealTarget> getSkyObject() {
        return skyObject;
    }

    public void setSkyObject(Option<SiderealTarget> skyObject) {
        if (skyObject == null) throw new IllegalArgumentException();
        this.skyObject = skyObject;
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
                + ", xView=" + xView
                + ", yView=" + yView
                + ", xWidget=" + xWidget
                + ", yWidget=" + yWidget
                + ", pos=" + pos
                + ", name=" + (name != null ? name : "<null>")
                + ", xOffset=" + xOffset
                + ", yOffset=" + yOffset + "]";
    }
}
