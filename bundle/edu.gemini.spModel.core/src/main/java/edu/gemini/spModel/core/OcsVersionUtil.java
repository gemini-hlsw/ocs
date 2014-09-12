package edu.gemini.spModel.core;

import java.io.File;

/**
 * An auxiliary class.
 */
public final class OcsVersionUtil {

    /**
     * Returns the directory where the programs associated to this release are stored.
     * The directory name is:  verion-name.public.internal
     *
     * @param parentDir parent directory of the return directory
     * @param version   version from where to get the directory name.
     * @return the Directory where the programs for this release are located
     */
    public static File getVersionDir(final File parentDir, final Version version) {
        final Semester semester = version.getSemester();
        final String testStr = version.isTest() ? "-test" : "";
        final int xml = version.getXmlCompatibility();
        final int ser = version.getSerialCompatibility();
        final String dbDir = String.format("%s%s.%d.%d", semester, testStr, xml, ser);
        return new File(parentDir, dbDir);
    }

}
