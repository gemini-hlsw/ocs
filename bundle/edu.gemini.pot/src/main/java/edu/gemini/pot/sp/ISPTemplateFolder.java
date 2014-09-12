package edu.gemini.pot.sp;


import java.util.List;

/**
 * The top-level Templates folder (if any) associated with an
 * {@link ISPProgram}.
 */
public interface ISPTemplateFolder extends ISPContainerNode, ISPProgramNode {
    String TEMPLATE_GROUP_PROP = "TemplateGroups";

    List<ISPTemplateGroup> getTemplateGroups() ;

    void setTemplateGroups(List<? extends ISPTemplateGroup> groupList)
        throws SPNodeNotLocalException, SPTreeStateException;

    void addTemplateGroup(ISPTemplateGroup group)
        throws SPNodeNotLocalException, SPTreeStateException;

    void addTemplateGroup(int index, ISPTemplateGroup group)
        throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException;

    void removeTemplateGroup(ISPTemplateGroup group);
}
