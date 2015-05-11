package edu.gemini.itc.base;

/**
 * Instruments that support binning should implement this interface.
 */
public interface BinningProvider {
    int getSpatialBinning();
    int getSpectralBinning();
}
