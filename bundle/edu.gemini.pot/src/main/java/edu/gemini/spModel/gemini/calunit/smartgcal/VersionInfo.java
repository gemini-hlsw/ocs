// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id$
//
package edu.gemini.spModel.gemini.calunit.smartgcal;

import java.io.Serializable;

public class VersionInfo implements Serializable {
    private String instrument;
    private Calibration.Type type;
    private Version version;

    public VersionInfo(String instrument, Calibration.Type type, Version version) {
        this.instrument = instrument;
        this.type = type;
        this.version = version;
    }

    public String getInstrument() {
        return instrument;
    }

    public Calibration.Type getType() {
        return type;
    }

    public Version getVersion() {
        return version;
    }

}

