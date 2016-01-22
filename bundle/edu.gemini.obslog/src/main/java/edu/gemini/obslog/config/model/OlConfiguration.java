package edu.gemini.obslog.config.model;

import java.util.Iterator;

public interface OlConfiguration {
    String getVersion();

    OlLogItem addLogItem(String itemKey);

    OlLogItem addItemToObsLog(String logKey, String itemKey) throws OlModelException;

    OlObsLogData getDataForLogByType(String narrowType);

    Iterator<String> getItems();
}

