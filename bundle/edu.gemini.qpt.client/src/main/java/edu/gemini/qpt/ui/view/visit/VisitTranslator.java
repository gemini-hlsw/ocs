package edu.gemini.qpt.ui.view.visit;

import java.util.Collections;
import java.util.Set;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.ui.gface.GTranslator;
import edu.gemini.ui.gface.GViewer;

public class VisitTranslator implements GTranslator<Variant, Alloc> {

    private Variant variant;
    
    public Set<Alloc> translate(Object o) {
        
        if (o instanceof Alloc)
            return Collections.singleton((Alloc) o);    

        if (o instanceof Marker)
            for (Object e: ((Marker) o).getPath())
                if (e instanceof Alloc)
                    return Collections.singleton((Alloc) e);
                
        if (o instanceof Obs && variant != null)
            return variant.getAllocs((Obs) o);
            
//    Hmm, this is a little too intense. Let's not do this.
//
//        if (o instanceof Variant && variant != null)
//            return variant.getAllocs();
        
        return Collections.emptySet();
    }

    public void modelChanged(GViewer<Variant, Alloc> viewer, Variant oldModel, Variant newModel) {
        variant = newModel;
    }

}
