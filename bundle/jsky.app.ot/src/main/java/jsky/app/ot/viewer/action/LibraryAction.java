package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPComponentType;
import jsky.app.ot.scilib.InstrumentGetterFunctor;
import jsky.app.ot.scilib.ScienceLibraryFetchListener;
import jsky.app.ot.scilib.ScienceLibraryFetcher;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.app.ot.viewer.ViewerManager;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;
import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 2:01 PM
* To change this template use File | Settings | File Templates.
*/
public final class LibraryAction extends AbstractViewerAction {

    private static final String DESCRIPTION =
        "Open locally stored libraries for the current program, they are " +
        "fetched if not available. Use File->Fetch Libraries to update local libraries.";

    public LibraryAction(SPViewer viewer) {
        super(viewer, "Libraries", jsky.util.Resources.getIcon("x-office-drawing-template.png", SPViewerActions.class));
        putValue(SHORT_DESCRIPTION, DESCRIPTION);
    }

    public void actionPerformed(ActionEvent evt) {
        try {

            ISPProgram prog = viewer.getProgram();
            if (prog == null) {
                return;
            }
            //Get the instruments in the program, fetch the libraries, open them in new windows
            //We don't update existing libraries with this mechanism
            List<SPComponentType> instruments = InstrumentGetterFunctor.getInstruments(prog);
            ScienceLibraryFetcher.fetchLibraries(instruments, false, new ScienceLibraryFetchListener() {
                public void fetchedLibrary(List<ISPProgram> libraries) {
                    //open the fetched libraries in a new window. If multiple
                    //libraries were returned, they will share the same window
                    SPViewer viewer1 = null;
                    for (ISPProgram program : libraries) {
                        viewer1 = ViewerManager.open(program, viewer1);
                    }
                }
            }, viewer.getParentFrame());
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        return getProgram() != null;
    }

}
