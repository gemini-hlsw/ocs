// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id$
//

package edu.gemini.spModel.gemini.calunit.smartgcal;


import java.io.Serializable;

public interface ConfigurationKey extends Serializable {

    String getInstrumentName();

    public interface Values extends Serializable {}

}
