// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SpIOTags.java 44507 2012-04-13 22:31:43Z rnorris $
//
package edu.gemini.spModel.io.impl;

/**
 * Provides a single source of tags for all SpItems to be used during
 * I/O operations.
 * <p>
 * The contents consists of a tag fetch routine as well as a number
 * of public attribute names.
 *
 * @author Kim Gillies (Gemini)
 */
public final class SpIOTags {

    // Old XML format tags
    public static final String ROOT_ELEMENT_TAG = "spDocument";
    public static final String PLAN_ELEMENT_TAG = "plan";
    public static final String PROGRAM_ELEMENT_TAG = "program";
    public static final String LIBRARY_ELEMENT_TAG = "library";
    public static final String OBSERVATION_ELEMENT_TAG = "observation";
    public static final String USEROBJ_ELEMENT_TAG = "userobj";
    public static final String PHASE1_ELEMENT_TAG = "phase1";

    public static final String OBSERVATION_NUMBER_ATTRIBUTE_TAG = "number";

    public static final String AV_ELEMENT_TAG = "av";
    public static final String AV_NAME_ATTRIBUTE_TAG = "name";
    public static final String AV_DESC_ATTRIBUTE_TAG = "descr";

    public static final String OBS_COMP_ITEM_ELEMENT_TAG = "obsComp";
    public static final String SEQ_COMP_ITEM_ELEMENT_TAG = "seqComp";
    public static final String ITEM_NAME_ATTRIBUTE_TAG = "name";
    public static final String ITEM_TYPE_ATTRIBUTE_TAG = "type";
    public static final String ITEM_SUBTYPE_ATTRIBUTE_TAG = "subtype";
    public static final String ITEM_KEY_ATTRIBUTE_TAG = "key";

    public static final String VALUE_ELEMENT_TAG = "value";
    public static final String VALUE_TYPE_TAG = "type";
    public static final String VALUE_UNITS_TAG = "units";

    public static final String ERROR_ATTRIBUTE_TAG = "error";


    // New XML format tags
    public static final String DOCUMENT = "document";
    public static final String CONTAINER = "container";
    public static final String PARAMSET = "paramset";
    public static final String PARAM = "param";
    public static final String VALUE = "value";
    public static final String UNITS = "units";
    public static final String KIND = "kind";
    public static final String EDITABLE = "editable";
    public static final String ACCESS = "access";
    public static final String TYPE = "type";
    public static final String SUBTYPE = "subtype";
    public static final String SEQUENCE = "sequence";
    public static final String REF = "ref";
    public static final String ID = "id";
    public static final String KEY = "key";
    public static final String NAME = "name";
    public static final String VERSION = "version";

    public static final String PROGRAM = "program";
    public static final String NIGHTLY_PLAN = "nightlyPlan";
    public static final String CONFLICT_FOLDER = "conflictFolder";
    public static final String PHASE1 = "phase1";
    public static final String GROUP = "group";
    public static final String TEMPLATE_FOLDER = "templateFolder";
    public static final String TEMPLATE_GROUP = "templateGroup";
    public static final String TEMPLATE_PARAMETERS = "templateParameters";
    public static final String OBSERVATION = "observation";
    public static final String OBS_QA_LOG    = "obsQaLog";
    public static final String OBS_EXEC_LOG  = "obsExecLog";
    public static final String OBSERVING_LOG = "obsLog";
    public static final String OBSCOMP = "obsComp";
    public static final String SEQCOMP = "seqComp";
    public static final String USEROBJ = "userObj";
    public static final String PUBLIC = "public";

}

