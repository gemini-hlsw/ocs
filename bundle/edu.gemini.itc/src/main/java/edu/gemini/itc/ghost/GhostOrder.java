package edu.gemini.itc.ghost;

import edu.gemini.itc.base.DatFile;

import java.util.Scanner;
import java.util.TreeMap;

public final class GhostOrder {
    static TreeMap<Float, Long> data= new TreeMap<Float, Long>();
    private static final String orderFilename = "blazeOrders";

    static {
        try (final Scanner scan = DatFile.scanFile(Ghost.INSTR_DIR + '/' + Ghost.INSTR_PREFIX + orderFilename + Ghost.DATA_SUFFIX)) {
            while (scan.hasNext())
                data.put(scan.nextFloat(), scan.nextLong());
        }
    }

}
