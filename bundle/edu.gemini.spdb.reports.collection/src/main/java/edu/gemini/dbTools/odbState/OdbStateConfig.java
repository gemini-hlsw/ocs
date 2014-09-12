//
// $Id: OdbStateConfig.java 4389 2004-01-29 17:19:14Z shane $
//
package edu.gemini.dbTools.odbState;

import java.io.File;

public class OdbStateConfig {

    public final File stateFile;

    public OdbStateConfig(final File tempDir) {
        stateFile = new File(tempDir, "odbState.xml");
    }

}
