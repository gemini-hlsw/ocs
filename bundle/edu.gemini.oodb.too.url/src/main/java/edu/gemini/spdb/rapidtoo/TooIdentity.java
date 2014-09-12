//
// $Id: TooIdentity.java 259 2006-01-13 20:35:11Z shane $
//

package edu.gemini.spdb.rapidtoo;

import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;

/**
 * Indentity of the program and observation to use in the rapid TOO update.
 * The program and password are used to authenticate the request, and the
 * template observation information is used to find the observation to clone
 * and update.
 */
public interface TooIdentity extends Serializable {

    /**
     * Gets the id of the TOO program that should be updated.
     *
     * @return program id corresponding to the program that will be updated
     */
    SPProgramID getProgramId();

    /**
     * Gets the email for the user submitting the request.
     *
     * @return the user email to match against the password of the
     * program in the database
     */
    String getEmail();

    /**
     * Gets the program password used to authenticate the request.
     *
     * @return the program password to match against the password of the
     * program in the database
     */
    String getPassword();

    /**
     * Gets the name of the template observation to clone.  Must match exactly
     * with an observation in the program (ignoring case).  May return
     * <code>null</code> if the {@link #getTemplateObsNumber() observation
     * number} is supplied.
     *
     * @return name of template observation, or <code>null</code> if the
     * observation number is supplied instead
     */
    String getTemplateObsName();

    /**
     * Gets the number of the template observation within the program that
     * should be cloned and updated.  May return <code>-1</code> to indicate
     * that an observation number has not been specified.  In this case, the
     * {@link #getTemplateObsName() observation name} should be spplied.
     *
     * @return number of the template observation, or <code>null</code> if the
     * name is supplied instead 
     */
    int getTemplateObsNumber();
}
