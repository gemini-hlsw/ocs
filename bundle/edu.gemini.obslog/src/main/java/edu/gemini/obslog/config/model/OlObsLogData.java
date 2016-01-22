package edu.gemini.obslog.config.model;

import edu.gemini.obslog.core.OlSegmentType;

import java.util.Iterator;
import java.util.List;

public interface OlObsLogData {

    String getKey();

    OlSegmentType getType();

    OlLogItem addLogItem(String key) throws OlModelException;

    Iterator<OlLogItem> iterator();

    List<OlLogItem> getLogTableData();

    int getSize();
}

