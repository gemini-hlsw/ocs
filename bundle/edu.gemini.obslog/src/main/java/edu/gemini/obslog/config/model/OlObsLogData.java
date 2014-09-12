package edu.gemini.obslog.config.model;

import edu.gemini.obslog.core.OlSegmentType;

import java.util.Iterator;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlObsLogData.java,v 1.2 2005/12/11 15:54:15 gillies Exp $
//

public interface OlObsLogData {

    public String getKey();

    public OlSegmentType getType();

    public OlLogItem addLogItem(String key) throws OlModelException;

    public OlLogItem getLogItem(String key);

    public Iterator iterator();

    public OlLogItem getBySequenceName(String sequenceKey);

    public List<OlLogItem> getLogTableData();

    public int getSize();
}

