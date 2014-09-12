package jsky.app.ot.tpe;

/**
 * Class for managing info/warning/error messages to be displayed in the image window.
 * See OT-33.
 */
public class TpeMessage implements Comparable<TpeMessage> {
    public enum TpeMessageType {INFO, WARNING, ERROR};
    private TpeMessageType messageType = TpeMessageType.INFO;
    private String message;

    public static TpeMessage infoMessage(String s) {
        return new TpeMessage(s, TpeMessageType.INFO);
    }
    public static TpeMessage warningMessage(String s) {
        return new TpeMessage(s, TpeMessageType.WARNING);
    }
    public static TpeMessage errorMessage(String s) {
        return new TpeMessage(s, TpeMessageType.ERROR);
    }

    public TpeMessage(String message, TpeMessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    public TpeMessageType getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int compareTo(TpeMessage o) {
        return message.compareTo(o.message);
    }

}
