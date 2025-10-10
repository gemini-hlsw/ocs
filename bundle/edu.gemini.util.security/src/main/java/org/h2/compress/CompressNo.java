/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.compress;

/**
 * This class implements a data compression algorithm that does in fact not
 * compress. This is useful if the data can not be compressed because it is
 * encrypted, already compressed, or random.
 */
public class CompressNo implements Compressor {

    public int getAlgorithm() {
        return Compressor.NO;
    }

    public void setOptions(String options) {
        // nothing to do
    }

    public int compress(byte[] in, int inLen, byte[] out, int outPos) {
        System.arraycopy(in, 0, out, outPos, inLen);
        return outPos + inLen;
    }

    public void expand(byte[] in, int inPos, int inLen, byte[] out, int outPos, int outLen) {
        System.arraycopy(in, inPos, out, outPos, outLen);
    }

}
