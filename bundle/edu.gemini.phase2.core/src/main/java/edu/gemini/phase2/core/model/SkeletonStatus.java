package edu.gemini.phase2.core.model;

public enum SkeletonStatus {
    NOT_PRESENT("Not Present", true),
    INITIALIZED("Initialized", true),
    MODIFIED("Modified", false),
    ;

    public final String display;
    public final boolean updatable;

    private SkeletonStatus(String display, boolean updatable) {
        this.display = display;
        this.updatable = updatable;
    }
}
