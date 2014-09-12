// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IApplyCancel.java 4726 2004-05-14 16:50:12Z brighton $

package jsky.util;

/**
 * An interface for dialogs that can be applied or canceled.
 */
public abstract interface IApplyCancel {
    public void apply();

    public void cancel();
}

