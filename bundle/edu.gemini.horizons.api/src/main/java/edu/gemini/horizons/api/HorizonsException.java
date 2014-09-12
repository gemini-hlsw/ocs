package edu.gemini.horizons.api;

/**
 * Exception to be thrown when there is a problem using the Horizons System
 */
public class HorizonsException extends Exception {

	private static final long serialVersionUID = 1L;

	private Type _type;

    public static enum Type {
        UNEXPECTED_RESULT("Unexpected Result");
        private String _description;

        Type(String desc) {
            _description = desc;
        }
        public String getDescription() {
            return _description;
        }
    }

    public static HorizonsException create(Exception wrapped) {
        if (wrapped instanceof HorizonsException) {
            return (HorizonsException) wrapped;
        }
        return new HorizonsException(wrapped.getMessage(), wrapped);
    }

    public static HorizonsException create(String message, Exception wrapped) {
        if (wrapped instanceof HorizonsException) {
            return (HorizonsException) wrapped;
        }
        return new HorizonsException(message, wrapped);
    }

    public static HorizonsException create(Type type, String message, Exception wrapped) {
        if (wrapped instanceof HorizonsException) {
            HorizonsException ex = (HorizonsException)wrapped;
            ex._type = type;
            return ex;
        }
        return new HorizonsException(type, message, wrapped);
    }

    public static HorizonsException create(Type type, Exception wrapped) {
        if (wrapped instanceof HorizonsException) {
            HorizonsException ex = (HorizonsException)wrapped;
            ex._type = type;
            return ex;
        }
        return new HorizonsException(type, wrapped);
    }


    private HorizonsException(String message, Exception wrapped) {
        super(message, wrapped);
    }

    public HorizonsException(String message) {
        super(message);
    }

    public HorizonsException(Type type) {
        super(type.getDescription());
        _type = type;
    }

    public HorizonsException(Type type, Exception wrapped) {
        super(type.getDescription(), wrapped);
        _type = type;
    }

    public HorizonsException(Type type, String message) {
        super(message);
        _type = type;
    }

    public HorizonsException(Type type, String message, Exception wrapped) {
        super(message, wrapped);
        _type = type;
    }



    public Type getType() {
        return _type;
    }
}
