package edu.gemini.itc.shared;

/**
 * Instruments that support binning should implement this interface.
 */
public interface BinningProvider {
    int getSpatialBinning();
    int getSpectralBinning();
}
