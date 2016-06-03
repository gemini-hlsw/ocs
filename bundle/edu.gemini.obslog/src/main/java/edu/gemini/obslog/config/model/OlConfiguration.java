package edu.gemini.obslog.config.model;

import java.util.Iterator;

//
// Gemini Observatory/AURA
// $Id: OlConfiguration.java,v 1.2 2004/12/01 15:03:00 gillies Exp $
//

public interface OlConfiguration {
    String getVersion();

    OlLogItem addLogItem(String itemKey);

    OlLogItem getLogItem(String itemKey);

    OlLogItem getItemInObsLog(String logKey, String itemKey);

    OlLogItem addItemToObsLog(String logKey, String itemKey) throws OlModelException;

    OlObsLogData getDataForLog(String logKey);

    OlObsLogData getDataForLogByType(String narrowType);

    int getNumberObsLogs();

    int getNumberLogItems();

    Iterator getObsLogItems(String logKey);

    Iterator getItems();
}

