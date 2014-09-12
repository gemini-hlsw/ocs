package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;

import java.io.IOException;
import java.util.List;

// A lame interface that closely matches the existing FileManager so as to
// easily allow one to choice between it and a truly transient database.

interface IDBPersister {
    List<ISPProgram> reloadPrograms() throws IOException;
    List<ISPNightlyRecord> reloadPlans() throws IOException;
    void store(ISPRootNode node) throws IOException;
    void remove(SPNodeKey key);

    /** Gets the size of the program file on disk, or -1 if not known. */
    long size(SPNodeKey key);

    /** Returns the total storage size, on disk, or zero for transient databases. */
    long getTotalStorage();
}
