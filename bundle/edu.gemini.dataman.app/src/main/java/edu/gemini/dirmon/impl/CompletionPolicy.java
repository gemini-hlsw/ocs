//
// $Id: CompletionPolicy.java 39 2005-08-20 22:40:25Z shane $
//

package edu.gemini.dirmon.impl;

import java.io.File;
import java.io.Serializable;

/**
 * Identifies a policy that is used to determine if a file is "complete".
 */
public interface CompletionPolicy extends Serializable {
    CompletionPolicy IMMEDIATE = new CompletionPolicy() {
        public boolean isComplete(File f) {
            return true;
        }
    };

    boolean isComplete(File f);
}
