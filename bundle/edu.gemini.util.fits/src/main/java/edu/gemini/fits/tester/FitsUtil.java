package edu.gemini.fits.tester;

import edu.gemini.fits.FitsParseException;
import edu.gemini.fits.Header;
import edu.gemini.fits.Hedit;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class to work with fits files
 */
public class FitsUtil {
    public static void main(String[] args) throws IOException, FitsParseException, InterruptedException {
        if (args.length > 0) {
            System.out.println("Parsing main header of " + args[0]);
            List<Header> headers = new Hedit(new File(args[0])).readAllHeaders();
            for (Header h:headers) {
                System.out.println(h);
            }
//            Set<String> keywords = pdu.getKeywords();
//            for(String keyword:keywords) {
//                System.out.println(pdu.get(keyword));
//            }
        }
    }
}
