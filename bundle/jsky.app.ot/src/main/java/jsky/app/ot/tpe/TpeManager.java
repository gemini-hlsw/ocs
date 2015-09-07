// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TpeManager.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;


/**
 * This class manages access to the TelescopePosEditor on behalf of clients.
 */
public final class TpeManager {

    /**
     * The single position editor, shared for all instances
     */
    private static TelescopePosEditor _tpe;


    /**
     * Return the position editor instance, if it exists, otherwise null.
     */
    public static TelescopePosEditor get() {
        return _tpe;
    }

    /**
     * Open the position editor window, creating it if necessary, and return a reference to it.
     */
    public static TelescopePosEditor open() {

        if (_tpe == null)
            _tpe = new TelescopePosEditor();

        _tpe.setImageFrameVisible(true);
        return _tpe;
    }

    /**
     * Create the position editor, if necessary, and return a reference to it.
     */
    public static TelescopePosEditor create() {

        if (_tpe == null)
            _tpe = new TelescopePosEditor();

        return _tpe;
    }

}
