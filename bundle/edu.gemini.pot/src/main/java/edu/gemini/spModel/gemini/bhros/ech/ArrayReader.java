package edu.gemini.spModel.gemini.bhros.ech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Reads an array from a file, skipping blank lines and lines starting with a
 * pound sign (#). Each line is passed to build(), which should be overridden 
 * to either return an object or null if the line is to be skipped.
 */
public abstract class ArrayReader<T> {

    public T[] readArray(InputStream is, T[] castTo) {
        try {
            ArrayList<T> accum = new ArrayList<>();
            InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.length() == 0)
                    continue;
                T o = build(line);
                if (o != null)
                    accum.add(o);
            }
            return accum.toArray(castTo);
        } catch (IOException ioe) {
            throw new RuntimeException("Trouble reading from " + is, ioe);
        }
    }

    protected abstract T build(String args);

}
