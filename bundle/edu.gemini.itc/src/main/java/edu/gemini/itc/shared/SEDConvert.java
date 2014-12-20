// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: SEDConvert.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;

/**
 * This class can be used to read through a SED data file and rewrite it.
 * User must modify the main() method and recompile.
 * An example use would be to convert a file from Angstroms to nm.
 */
public class SEDConvert {
    /**
     * Modify this method to do your conversion and recompile.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("SEDConvert inputFile [outputFile]");
            return;
        }
        String inputFile = args[0];
        String outputFile = null;
        if (args.length > 1) outputFile = args[1];

        PrintStream out;
        if (outputFile == null) {
            out = new PrintStream(System.out);
        } else {
            try {
                out = new PrintStream(new FileOutputStream(outputFile));
            } catch (Exception e) {
                System.out.println("Failed to open output file " + outputFile);
                System.out.println(e.toString());
                return;
            }
        }

        TextFileReader dfr = null;
        try {
            dfr = new TextFileReader(inputFile);
        } catch (Exception e) {
            System.out.println("SED file not found: " + inputFile);
            return;
        }

        double wavelength = 0;
        double energy = 0;

        try {
            while (true) {
                wavelength = dfr.readDouble();
                out.print(" ");
                wavelength /= 10.0;
                out.print(wavelength);
                out.print("  ");
                energy = dfr.readDouble();
                out.print(energy);
                out.println();
            }
        } catch (ParseException e) {
            System.out.println(e.toString());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }
}
