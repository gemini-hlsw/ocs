package edu.gemini.auxfile.server;


import java.io.Serializable;
import java.util.Arrays;

/**
 * A chunk of data from a file.  AuxFileChunk is used in the transfer to and
 * from remote servers.
 */
public final class AuxFileChunk implements Serializable {
    private final int    _chunkNumber;
    private final int    _chunkSize;
    private final long   _fileSize;
    private final long   _timestamp;
    private final byte[] _chunkData;

    public AuxFileChunk(int chunkNumber, int chunkSize, long fileSize,
                        long timestamp, byte[] chunkData) {
        if (chunkNumber < 0) {
            throw new IllegalArgumentException("chunk number: " + chunkNumber);
        }
        if (chunkSize < 0) {
            throw new IllegalArgumentException("chunk size: " + chunkSize);
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize: " + fileSize);
        }
        if (chunkData == null) {
            throw new NullPointerException("chunk data is null");
        }

        _chunkNumber = chunkNumber;
        _chunkSize   = chunkSize;
        _fileSize    = fileSize;
        _timestamp   = timestamp;
        _chunkData   = chunkData;
    }

    public int getChunkNumber() { return _chunkNumber; }

    public int getTotalChunks() {
        return (int) Math.ceil(((double) _fileSize) / _chunkSize);
    }

    public byte[] getChunkData() { return _chunkData; }

    public boolean isLastChunk() {
        return ((_chunkNumber+1) * _chunkSize) >= _fileSize;
    }

    public long getFileSize() { return _fileSize; }

    public long getTimestamp() { return _timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuxFileChunk that = (AuxFileChunk) o;

        if (_chunkNumber != that._chunkNumber) return false;
        if (_chunkSize != that._chunkSize) return false;
        if (_fileSize != that._fileSize) return false;
        if (_timestamp != that._timestamp) return false;
        if (!Arrays.equals(_chunkData, that._chunkData)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _chunkNumber;
        result = 31 * result + _chunkSize;
        result = 31 * result + (int) (_fileSize ^ (_fileSize >>> 32));
        result = 31 * result + (int) (_timestamp ^ (_timestamp >>> 32));
        result = 31 * result + Arrays.hashCode(_chunkData);
        return result;
    }
}
