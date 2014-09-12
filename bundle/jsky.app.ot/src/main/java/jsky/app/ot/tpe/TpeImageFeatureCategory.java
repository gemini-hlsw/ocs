package jsky.app.ot.tpe;

/**
 * Image feature categories
 */
public enum TpeImageFeatureCategory {
    target("Target"),
    fieldOfView("Field Of View"),
    ;

    private final String display;

    private TpeImageFeatureCategory(String display) {
        this.display = display;
    }

    public String displayName() {
        return display;
    }
}
