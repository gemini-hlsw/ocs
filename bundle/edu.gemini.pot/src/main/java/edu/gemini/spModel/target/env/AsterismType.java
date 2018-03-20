package edu.gemini.spModel.target.env;

import edu.gemini.spModel.gemini.ghost.GhostAsterism$;

public enum AsterismType {
    Single("single") {
        @Override
        public Asterism createEmptyAsterism() {
            return Asterism$.MODULE$.createSingleAsterism();
        }
    },
    GhostStandardResolution("ghostStandardRes") {
        @Override
        public Asterism createEmptyAsterism() {
            return GhostAsterism$.MODULE$.createEmptyStandardResolutionAsterism();
        }
    },
    GhostHighResolution("ghostHighRes") {
        @Override
        public Asterism createEmptyAsterism() {
            return GhostAsterism$.MODULE$.createEmptyHighResolutionAsterism();
        }
    }
    ;

    public final String tag;

    AsterismType(final String tag) {
        this.tag = tag;
    }

    public abstract Asterism createEmptyAsterism();
}
