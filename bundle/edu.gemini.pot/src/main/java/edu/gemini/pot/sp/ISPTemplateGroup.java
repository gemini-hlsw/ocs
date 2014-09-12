package edu.gemini.pot.sp;


import java.util.List;

/**
 * The template group which corresponds to a Phase 1 resource.
 */
public interface ISPTemplateGroup extends ISPContainerNode, ISPObservationContainer, ISPObsComponentContainer, ISPProgramNode {

    /** Marker interface for ISP nodes that are valid children. */
    interface Child {
    }

    String TEMPLATE_OBSERVATIONS_PROP = "TemplateObservations";
    String TEMPLATE_PARAMETERS_PROP = "TemplateParameters";

    // Template parameters

    List<ISPTemplateParameters> getTemplateParameters() ;

    void setTemplateParameters(List<? extends ISPTemplateParameters> paramsList)
            throws SPNodeNotLocalException, SPTreeStateException;

    void addTemplateParameters(ISPTemplateParameters params)
            throws SPNodeNotLocalException, SPTreeStateException;

    void addTemplateParameters(int index, ISPTemplateParameters params)
            throws SPNodeNotLocalException, SPTreeStateException;

    void removeTemplateParameters(ISPTemplateParameters params) ;


}
