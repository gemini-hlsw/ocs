package jsky.app.ot.scilib;

import edu.gemini.pot.sp.ISPProgram;

import java.util.List;

/**
 * Callback interface to notify GUI event thread when science libraries fetch operation
 * was finished
 */
public interface ScienceLibraryFetchListener {
     /** Called when science libraries have been fetched */
    public void fetchedLibrary(List<ISPProgram> libraries);
}
