// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: IDBProgramLister.java 7397 2006-10-17 13:55:11Z shane $
//

package edu.gemini.spModel.util;

import edu.gemini.pot.spdb.IDBQueryFunctor;

import java.util.List;


/**
 * An interface for a specialized type of
 * <code>edu.gemini.pot.spdb.IDBQueryFunctor</code>
 * implementation that can be used by clients to obtain a listing of all
 * the available program names and IDs ({@link DBProgramInfo}) fitting a particular search algorithm.
 */
public interface IDBProgramLister extends IDBQueryFunctor {

    /**
     * Gets the list of program names and IDs.  The result should be the accumulation
     * of program names and ids that have passed the query specified in
     * <code>execute</code>
     *
     * @return a list of DBProgramInfo objects
     */
    public List<DBProgramInfo> getList();
}

