package edu.gemini.obslog.obslog;

//
// Gemini Observatory/AURA
// $Id: OlLogInformation.java,v 1.1 2005/02/14 21:08:42 gillies Exp $
//

public final class OlLogInformation {

    public static final String DEFAULT_VALUE = "";

    private String _nightObservers = DEFAULT_VALUE;
    private String _ssas = DEFAULT_VALUE;
    private String _dataproc = DEFAULT_VALUE;
    private String _dayobs = DEFAULT_VALUE;
    private String _nightComment = DEFAULT_VALUE;

    private String _filePrefix = DEFAULT_VALUE;
    private String _ccVersion = DEFAULT_VALUE;
    private String _dcVersion = DEFAULT_VALUE;
    private String _softwareComment = DEFAULT_VALUE;


    public void setNightObservers(String nightObservers) {
        _nightObservers = nightObservers;
    }

    public String getNightObservers() {
        return _nightObservers;
    }

    public void setSsas(String ssas) {
        _ssas = ssas;
    }

    public String getSsas() {
        return _ssas;
    }

    public void setDataproc(String dataproc) {
        _dataproc = dataproc;
    }

    public String getDataproc() {
        return _dataproc;
    }

    public void setDayobserver(String dayobs) {
        _dayobs = dayobs;
    }

    public String getDayobserver() {
        return _dayobs;
    }

    public void setNightComment(String nightComment) {
        _nightComment = nightComment;
    }

    public String getNightComment() {
        return _nightComment;
    }

    public void setFilePrefix(String filePrefix) {
        _filePrefix = filePrefix;
    }

    public String getFilePrefix() {
        return _filePrefix;
    }

    public void setCCVersion(String ccVersion) {
        _ccVersion = ccVersion;
    }

    public String getCCVersion() {
        return _ccVersion;
    }

    public void setDCVersion(String dcVersion) {
        _dcVersion = dcVersion;
    }

    public String getDCVersion() {
        return _dcVersion;
    }

    public void setSoftwareComment(String softwareComment) {
        _softwareComment = softwareComment;
    }

    public String getSoftwareComment() {
        return _softwareComment;
    }

}
