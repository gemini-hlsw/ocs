//
// $Id: TooUpdateService.java 259 2006-01-13 20:35:11Z shane $
//

package edu.gemini.spdb.rapidtoo;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.spdb.IDBDatabaseService;



/**
 * An interface that describes a service used to update TOO observations.
 */
public interface TooUpdateService {

    /**
     * Handles a TOO update request, authenticating the request, locating and
     * cloning the specified template observation, and updating the newly
     * created observation with target and position angle information.
     *
     * @param db database to use to perform the update
     *
     * @param update description of the update to apply
     *
     * @return the newly created observation cloned from the template
     *
     * @throws AuthenticationException if the program id and password do not
     * match the database
     *
     * @throws InactiveProgramException if the program is not active (and
     * therefore cannot be updated)
     *
     * @throws MissingTemplateException if the template observation cannot be
     * identified
     *
     * @ if there is a problem communicating with the
     * database
     */
    ISPObservation handleUpdate(IDBDatabaseService db, TooUpdate update)
            throws TooUpdateException;
}
