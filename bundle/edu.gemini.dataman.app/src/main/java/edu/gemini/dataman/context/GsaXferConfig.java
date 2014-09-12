package edu.gemini.dataman.context;

public interface GsaXferConfig extends XferConfig {

    String XFER_MD_INGEST_SCRIPT = "edu.gemini.dataman.xfer.mdIngestScript";
    String XFER_CADC_ROOT        = "edu.gemini.dataman.xfer.cadcRoot";
    String XFER_CADC_GROUP       = "edu.gemini.dataman.xfer.cadcGroup";

    /**
     * Returns the path to the mdIngest script, as specified in bundles properties.
     */
    String getMdIngestScript();

    /**
     * Returns the value to use for CADC_ROOT, which is required by the
     * mdIngets script.
     */
    String getCadcRoot();

    /**
     * Returns the group that should be used for files transfered to the GSA.
     */
    String getCadcGroup();
}
