package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;


public final class TemplateParametersNI implements ISPNodeInitializer {

    public void initNode(ISPFactory factory, ISPNode node)  {
        node.setDataObject(new TemplateParameters());
    }

    public void updateNode(ISPNode node)  {
    }

}
